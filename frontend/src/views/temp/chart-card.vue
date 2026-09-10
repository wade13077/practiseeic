<script setup lang="ts">
interface Props {
  cardClass?: string
}

withDefaults(defineProps<Props>(), {
  cardClass: 'card r20 pad12',
})
</script>

<template>
  <article :class="cardClass">
    <div class="card-head">
      <div class="card-title">
        <slot name="title" />
      </div>
    </div>
    <slot />
  </article>
</template>