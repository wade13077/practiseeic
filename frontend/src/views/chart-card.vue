<script setup lang="ts">
interface Props {
    cardClass?: string
}

withDefaults(defineProps<Props>(), {
    cardClass: 'card r20 pad12',
})

// defineProps<Props>()：宣告這個元件有哪些 Props。這裡使用 TypeScript 的泛型語法，傳入自訂的型別介面 Props（如 interface Props { cardClass?: string }）。

// withDefaults(...)：Vue 3 提供的高階函式（Compiler Macro）。因為單純寫 defineProps<Props>() 只能定義「型別」，無法直接指定預設值，所以必須用 withDefaults 包裹起來。

// 第二個參數物件 { cardClass: 'card r20 pad12' }：指定當父元件沒有傳入 cardClass 時，預設會使用的樣式名稱。
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


<!--  父元件使用時
<chart-card />    等於    <article class="card r20 pad12"></article>
<chart-card cardClass="custom-style" />    等於    <article class="custom-style"></article> 


-->