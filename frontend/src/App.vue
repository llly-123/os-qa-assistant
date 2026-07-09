<template>
  <router-view />
</template>

<script setup>
import { onMounted } from 'vue'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

onMounted(() => {
  // 拉取品牌化设置（站点名/课程名/学校名），用于全局展示与文档标题
  userStore.fetchPublicSettings().then(() => {
    document.title = userStore.siteName || '智能答疑助手'
  })
})
</script>

<style>
html, body, #app {
  margin: 0;
  padding: 0;
  height: 100%;
  width: 100%;
}
</style>
