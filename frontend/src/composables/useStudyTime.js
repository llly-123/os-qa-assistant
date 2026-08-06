import { onMounted, onBeforeUnmount } from 'vue'
import { useChatStore } from '@/stores/chat'

// 学习时长上报周期（毫秒）：每隔 60s 将累计的在线秒数上报一次
const REPORT_INTERVAL = 60 * 1000
// 单批上报秒数上限（后端单次上报上限为 1800 秒，留出余量避免被截断丢秒）
const REPORT_BATCH_SECONDS = 1500

/**
 * 学习时长统计（useStudyTime）
 * 学生在课程界面（已选择班级）停留期间，按秒累计在线时间并周期性上报；
 * 页面隐藏/关闭时立即上报剩余秒数，保证计时不丢失。
 *
 * 注意：上报使用原生 fetch 而非全局 axios 实例，避免 401 时触发
 * request.js 拦截器的"清空 token + 跳转登录页"逻辑，把学生踢出班级。
 *
 * @param shouldTrack 返回 boolean 的函数：是否处于"课程界面"（学生角色且已选班级）
 */
export function useStudyTime(shouldTrack) {
  const chatStore = useChatStore()

  let accumulated = 0        // 已累计但尚未上报的秒数
  let lastActiveTime = null  // 上一次计入时长的时刻（页面可见时）
  let visible = !document.hidden
  let timer = null

  // 原生 fetch 上报（带 token），不走 axios 全局拦截器
  function reportViaFetch(seconds, classId) {
    const token = localStorage.getItem('token')
    return fetch('/api/chat/study-time', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: token ? `Bearer ${token}` : ''
      },
      body: JSON.stringify({ classId, seconds })
    }).then(res => {
      if (!res.ok) throw new Error('study-time report failed: ' + res.status)
    })
  }

  // 将自 lastActiveTime 起的可见时长累加进 accumulated
  function markElapsed(forceActive) {
    const now = Date.now()
    if (lastActiveTime == null) {
      lastActiveTime = now
      return
    }
    if ((visible || forceActive) && shouldTrack()) {
      const elapsed = Math.floor((now - lastActiveTime) / 1000)
      if (elapsed > 0) accumulated += elapsed
    }
    lastActiveTime = now
  }

  // 周期性上报（失败则整批回滚，下轮重试；超过单批上限时拆分上报避免截断）
  async function flush() {
    markElapsed(false)
    const seconds = accumulated
    const classId = chatStore.currentClassId
    accumulated = 0
    lastActiveTime = Date.now()
    if (seconds <= 0 || !classId) return
    try {
      let rest = seconds
      while (rest > 0) {
        const batch = Math.min(rest, REPORT_BATCH_SECONDS)
        await reportViaFetch(batch, classId)
        rest -= batch
      }
    } catch (e) {
      accumulated += seconds
    }
  }

  // 页面隐藏/关闭前用 keepalive 请求立即上报，避免丢失最后一段时长；
  // 超过单批上限时拆成多批 keepalive 请求（浏览器会自动排队，总量在 keepalive 64KB 限制内）
  function flushFinal() {
    markElapsed(true)
    const seconds = accumulated
    const classId = chatStore.currentClassId
    accumulated = 0
    if (seconds <= 0 || !classId) return
    const token = localStorage.getItem('token')
    try {
      let rest = seconds
      while (rest > 0) {
        const batch = Math.min(rest, REPORT_BATCH_SECONDS)
        fetch('/api/chat/study-time', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            Authorization: token ? `Bearer ${token}` : ''
          },
          body: JSON.stringify({ classId, seconds: batch }),
          keepalive: true
        }).catch(() => {})
        rest -= batch
      }
    } catch (e) { /* ignore */ }
  }

  function handleVisibility() {
    if (document.hidden) {
      visible = false
      flushFinal() // 内部会 markElapsed(true) 计入隐藏前的一段时间
      lastActiveTime = Date.now()
    } else {
      visible = true
      lastActiveTime = Date.now()
    }
  }

  onMounted(() => {
    lastActiveTime = Date.now()
    document.addEventListener('visibilitychange', handleVisibility)
    window.addEventListener('pagehide', flushFinal)
    timer = setInterval(flush, REPORT_INTERVAL)
  })

  onBeforeUnmount(() => {
    clearInterval(timer)
    document.removeEventListener('visibilitychange', handleVisibility)
    window.removeEventListener('pagehide', flushFinal)
    flushFinal()
  })

  return { flush }
}
