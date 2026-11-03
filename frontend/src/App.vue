<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import DreamScene from '@/components/DreamScene.vue'
import BottomNav from '@/components/BottomNav.vue'
import LevelUpModal from '@/components/LevelUpModal.vue'

const route = useRoute()

const HIDDEN_NAV_ROUTES = new Set(['welcome', 'create-pet', 'chat'])

const showNav = computed(() => !HIDDEN_NAV_ROUTES.has(String(route.name)))
</script>

<template>
  <DreamScene />

  <RouterView v-slot="{ Component }">
    <Transition name="fade" mode="out-in">
      <component :is="Component" />
    </Transition>
  </RouterView>

  <BottomNav v-if="showNav" />
  <LevelUpModal />
</template>