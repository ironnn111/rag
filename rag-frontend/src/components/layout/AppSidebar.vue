<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'
import { computed } from 'vue'

const route = useRoute()
const router = useRouter()

const activeIndex = computed(() => route.path)

const menuItems = [
  { path: '/', title: 'RAG 问答', icon: 'ChatDotRound' },
  { path: '/documents', title: '文档管理', icon: 'Document' },
]
</script>

<template>
  <div class="sidebar">
    <div class="sidebar-logo">
      <span class="logo-icon">R</span>
      <span class="logo-text">RAG Admin</span>
    </div>
    <el-menu
      :default-active="activeIndex"
      background-color="#1d1e2c"
      text-color="#a6a7b3"
      active-text-color="#fff"
      @select="(index: string) => router.push(index)"
    >
      <el-menu-item v-for="item in menuItems" :key="item.path" :index="item.path">
        <el-icon><component :is="item.icon" /></el-icon>
        <span>{{ item.title }}</span>
      </el-menu-item>
    </el-menu>
  </div>
</template>

<style scoped>
.sidebar {
  height: 100%;
  display: flex;
  flex-direction: column;
}
.sidebar-logo {
  height: 56px;
  display: flex;
  align-items: center;
  padding: 0 20px;
  gap: 10px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}
.logo-icon {
  width: 32px;
  height: 32px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-weight: 700;
  font-size: 16px;
}
.logo-text {
  color: #fff;
  font-size: 17px;
  font-weight: 600;
}
.el-menu {
  border-right: none;
  flex: 1;
}
</style>
