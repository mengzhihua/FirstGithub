<template>
  <el-container style="height: 100%">
    <el-aside width="220px" style="background: #1f2d3d; color: #fff">
      <div style="height: 56px; display: flex; align-items: center; padding: 0 16px; font-size: 18px; font-weight: 600">
        <el-icon style="margin-right: 8px"><Box /></el-icon> WMS 仓储管理
      </div>
      <el-menu :default-active="route.path" router background-color="#1f2d3d" text-color="#bfcbd9" active-text-color="#409eff" :default-openeds="menus.filter(m => m.children).map(m => m.path)">
        <template v-for="m in menus" :key="m.path">
          <el-sub-menu v-if="m.children" :index="m.path">
            <template #title><el-icon><component :is="m.icon" /></el-icon><span>{{ m.name }}</span></template>
            <el-menu-item v-for="c in m.children" :key="c.path" :index="`${m.path}/${c.path}`">{{ c.name }}</el-menu-item>
          </el-sub-menu>
          <el-menu-item v-else :index="m.path"><el-icon><component :is="m.icon" /></el-icon><span>{{ m.name }}</span></el-menu-item>
        </template>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header style="background: #fff; border-bottom: 1px solid #e4e7ed; display: flex; align-items: center; justify-content: space-between; height: 56px">
        <el-breadcrumb separator="/">
          <el-breadcrumb-item v-for="b in crumbs" :key="b">{{ b }}</el-breadcrumb-item>
        </el-breadcrumb>
        <span style="color: #606266"><el-icon><User /></el-icon> admin</span>
      </el-header>
      <el-main style="padding: 0; overflow: auto">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { menus } from '../router'

const route = useRoute()
const crumbs = computed(() => {
  for (const m of menus) {
    if (m.children) {
      const c = m.children.find((c) => `${m.path}/${c.path}` === route.path)
      if (c) return [m.name, c.name]
    } else if (m.path === route.path) return [m.name]
  }
  return []
})
</script>
