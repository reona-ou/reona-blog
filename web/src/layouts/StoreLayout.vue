<template>
  <div class="store-shell">
    <SiteHeader />
    <main class="store-main">
      <router-view v-slot="{ Component }">
        <transition name="page" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </main>
    <SiteFooter />
    <ChatWidget />
  </div>
</template>

<script setup>
import SiteHeader from '@/components/SiteHeader.vue'
import SiteFooter from '@/components/SiteFooter.vue'
import ChatWidget from '@/components/ChatWidget.vue'
</script>

<style scoped>
.store-shell {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.store-main {
  flex: 1;
  padding-top: 76px;
}

.page-enter-active,
.page-leave-active {
  transition: opacity 0.22s ease, transform 0.22s ease;
}

.page-enter-from {
  opacity: 0;
  transform: translateY(8px);
}

.page-leave-to {
  opacity: 0;
}
</style>
