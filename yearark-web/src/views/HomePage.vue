<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  Book, BookOpen, ArrowRight,
  Camera, Users, Sparkles, Zap,
  Upload, Wand2, Download,
  Heart, Star, Quote
} from 'lucide-vue-next'
import { Button } from '@/components/ui/button'
import Navbar from '@/components/Navbar.vue'
import { useUserStore } from '@/stores/user'
import { listPublicAlbums } from '@/api/public'

interface PublicAlbum {
  id: number
  name: string
  des: string
  userName?: string
  createAt: string
}

const router = useRouter()
const userStore = useUserStore()
const albums = ref<PublicAlbum[]>([])
const loading = ref(false)

const cardHeights = ['h-64', 'h-72', 'h-80', 'h-56', 'h-96', 'h-60']
function randomHeight(index: number) {
  return cardHeights[index % cardHeights.length]
}

const gradients = [
  'from-primary/10 to-primary/5',
  'from-amber-50/60 to-orange-50/30',
  'from-stone-100/60 to-amber-50/40',
  'from-orange-50/50 to-yellow-50/30',
  'from-rose-50/40 to-amber-50/30',
  'from-stone-50/60 to-stone-100/40',
]
function randomGradient(index: number) {
  return gradients[index % gradients.length]
}

const features = [
  { icon: Camera, title: '多人协作上传', desc: '生成邀请链接，同学扫码即可上传照片和寄语，轻松收集素材。' },
  { icon: Sparkles, title: 'AI 智能排版', desc: 'AI 自动匹配照片与文字，智能生成精美版面，省去繁琐排版。' },
  { icon: Zap, title: '一键生成', desc: '选择模板，点击生成，几分钟内即可获得专属纪念册 PDF。' },
  { icon: Users, title: '丰富模板', desc: '多种精心设计的模板风格，总有一款适合你的青春故事。' },
]

const steps = [
  { num: '01', title: '创建纪念册', desc: '注册账号，填写纪念册名称和描述，选择心仪的模板。', icon: Book },
  { num: '02', title: '邀请同学', desc: '生成专属邀请链接，分享给同学，大家一起上传照片和留言。', icon: Upload },
  { num: '03', title: 'AI 排版生成', desc: 'AI 自动将素材排版到模板中，你也可以手动微调每一页。', icon: Wand2 },
  { num: '04', title: '下载分享', desc: '一键导出高清 PDF，打印或在线分享给每一位同学。', icon: Download },
]

const testimonials = [
  { name: '小林', role: '2025 届毕业生', text: '毕业前一周才开始做纪念册，没想到这么快就搞定了，同学们都说排版很好看。', avatar: '林' },
  { name: '阿杰', role: '班长', text: '以前用 PPT 做纪念册累死了，这个工具邀请链接一发，大家自己传照片，太省心了。', avatar: '杰' },
  { name: '小雨', role: '设计专业', text: '模板质量很高，AI 排版出来的效果比我预期的好很多，细节也可以自己调。', avatar: '雨' },
]

async function fetchPublicAlbums() {
  loading.value = true
  try {
    const res = await listPublicAlbums()
    albums.value = (res.data as any)?.data ?? []
  } catch (err) {
    console.error('Failed to fetch public albums', err)
    albums.value = []
  } finally {
    loading.value = false
  }
}

function scrollToAlbums() {
  document.getElementById('public-albums')?.scrollIntoView({ behavior: 'smooth' })
}

function handleAlbumClick(id: number) {
  if (userStore.isLoggedIn) {
    router.push(`/album/${id}/preview`)
  } else {
    router.push('/login')
  }
}

function formatDate(dateStr: string) {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric' })
}

onMounted(() => {
  userStore.loadFromStorage()
  fetchPublicAlbums()
})
</script>

<template>
  <div class="min-h-screen bg-background font-sans text-foreground">
    <!-- Shared Navbar -->
    <Navbar />

    <!-- Hero Banner -->
    <section class="relative overflow-hidden bg-gradient-to-br from-primary/5 via-background to-accent/30">
      <!-- Decorative blobs -->
      <div class="absolute top-10 left-10 w-20 h-20 rounded-full bg-primary/5 blur-2xl" />
      <div class="absolute bottom-10 right-10 w-32 h-32 rounded-full bg-accent/20 blur-3xl" />
      <div class="absolute top-1/2 left-1/3 w-40 h-40 rounded-full bg-amber-100/20 blur-3xl" />

      <div class="container mx-auto max-w-7xl px-6 py-24 md:py-36 flex flex-col items-center text-center relative z-10">
        <div class="inline-flex items-center gap-2 px-4 py-1.5 rounded-full bg-primary/10 text-primary text-sm font-medium mb-6">
          <BookOpen class="w-4 h-4" />
          留住青春，珍藏回忆
        </div>
        <h1 class="text-4xl md:text-6xl font-serif font-bold text-foreground leading-tight" style="letter-spacing: 0.15em;">
          用一本纪念册<br />
          <span class="text-primary">定格你的故事</span>
        </h1>
        <p class="mt-6 text-lg md:text-xl text-muted-foreground max-w-2xl mx-auto leading-relaxed">
          YearArk 帮你轻松制作精美的毕业纪念册，邀请同学一起上传照片和寄语，AI 智能排版，一键生成专属回忆。
        </p>
        <div class="mt-10 flex items-center gap-4 justify-center">
          <Button size="lg" class="gap-2 shadow-lg text-base px-8" @click="router.push(userStore.isLoggedIn ? '/dashboard' : '/register')">
            开始创建
            <ArrowRight class="w-4 h-4" />
          </Button>
          <Button size="lg" variant="outline" class="text-base px-8" @click="scrollToAlbums">
            浏览作品
          </Button>
        </div>

        <!-- Stats row -->
        <div class="mt-16 grid grid-cols-3 gap-8 md:gap-16">
          <div class="text-center">
            <div class="text-3xl md:text-4xl font-serif font-bold text-primary">500+</div>
            <div class="text-sm text-muted-foreground mt-1">纪念册已生成</div>
          </div>
          <div class="text-center">
            <div class="text-3xl md:text-4xl font-serif font-bold text-primary">2000+</div>
            <div class="text-sm text-muted-foreground mt-1">用户信赖</div>
          </div>
          <div class="text-center">
            <div class="text-3xl md:text-4xl font-serif font-bold text-primary">50k+</div>
            <div class="text-sm text-muted-foreground mt-1">照片已收录</div>
          </div>
        </div>
      </div>
    </section>

    <!-- Features Section -->
    <section class="bg-card/50 border-y">
      <div class="container mx-auto max-w-7xl px-6 py-20">
        <div class="text-center mb-14">
          <h2 class="text-3xl font-serif font-bold text-foreground">为什么选择 YearArk</h2>
          <p class="mt-3 text-muted-foreground max-w-lg mx-auto">从素材收集到成品输出，我们把每一步都变得简单</p>
        </div>
        <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-8">
          <div
            v-for="(f, i) in features"
            :key="i"
            class="group p-6 rounded-xl bg-background border hover:border-primary/30 hover:shadow-md transition-all duration-300"
          >
            <div class="w-12 h-12 rounded-lg bg-primary/10 flex items-center justify-center mb-4 group-hover:bg-primary/20 transition-colors">
              <component :is="f.icon" class="w-6 h-6 text-primary" />
            </div>
            <h3 class="font-serif font-bold text-lg text-foreground mb-2">{{ f.title }}</h3>
            <p class="text-sm text-muted-foreground leading-relaxed">{{ f.desc }}</p>
          </div>
        </div>
      </div>
    </section>

    <!-- How It Works -->
    <section class="container mx-auto max-w-7xl px-6 py-20">
      <div class="text-center mb-14">
        <h2 class="text-3xl font-serif font-bold text-foreground">四步完成你的纪念册</h2>
        <p class="mt-3 text-muted-foreground">简单几步，把回忆变成可以翻阅的故事</p>
      </div>
      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <div
          v-for="(s, i) in steps"
          :key="i"
          class="relative p-6 rounded-xl border bg-card hover:shadow-md transition-all group"
        >
          <!-- Step number -->
          <div class="text-5xl font-serif font-bold text-primary/10 absolute top-4 right-4 group-hover:text-primary/20 transition-colors">
            {{ s.num }}
          </div>
          <div class="w-10 h-10 rounded-lg bg-primary/10 flex items-center justify-center mb-4">
            <component :is="s.icon" class="w-5 h-5 text-primary" />
          </div>
          <h3 class="font-serif font-bold text-base text-foreground mb-2">{{ s.title }}</h3>
          <p class="text-sm text-muted-foreground leading-relaxed">{{ s.desc }}</p>
          <!-- Connector arrow (not on last) -->
          <div v-if="i < steps.length - 1" class="hidden lg:block absolute -right-3 top-1/2 -translate-y-1/2 z-10">
            <ArrowRight class="w-5 h-5 text-border" />
          </div>
        </div>
      </div>
    </section>

    <!-- Testimonials -->
    <section class="bg-gradient-to-b from-accent/20 to-background border-t">
      <div class="container mx-auto max-w-7xl px-6 py-20">
        <div class="text-center mb-14">
          <h2 class="text-3xl font-serif font-bold text-foreground">他们都在用 YearArk</h2>
          <p class="mt-3 text-muted-foreground">听听同学们怎么说</p>
        </div>
        <div class="grid grid-cols-1 md:grid-cols-3 gap-8">
          <div
            v-for="(t, i) in testimonials"
            :key="i"
            class="p-6 rounded-xl bg-card border shadow-sm hover:shadow-md transition-all"
          >
            <Quote class="w-8 h-8 text-primary/15 mb-3" />
            <p class="text-sm text-foreground leading-relaxed mb-5">{{ t.text }}</p>
            <div class="flex items-center gap-3 pt-4 border-t">
              <div class="w-9 h-9 rounded-full bg-primary/10 flex items-center justify-center text-primary font-serif font-bold text-sm">
                {{ t.avatar }}
              </div>
              <div>
                <div class="text-sm font-medium text-foreground">{{ t.name }}</div>
                <div class="text-xs text-muted-foreground">{{ t.role }}</div>
              </div>
              <div class="ml-auto flex gap-0.5">
                <Star v-for="s in 5" :key="s" class="w-3.5 h-3.5 text-amber-400 fill-amber-400" />
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- Public Albums Waterfall -->
    <section id="public-albums" class="container mx-auto max-w-7xl px-6 py-20">
      <div class="text-center mb-12">
        <h2 class="text-3xl font-serif font-bold text-foreground">精选纪念册</h2>
        <p class="mt-3 text-muted-foreground">来自社区的优秀作品，看看大家的青春故事</p>
      </div>

      <!-- Loading -->
      <div v-if="loading" class="columns-1 sm:columns-2 lg:columns-3 xl:columns-4 gap-5 space-y-5">
        <div v-for="i in 8" :key="i" class="break-inside-avoid rounded-xl bg-muted animate-pulse"
             :class="i % 2 === 0 ? 'h-72' : 'h-56'" />
      </div>

      <!-- Empty -->
      <div v-else-if="albums.length === 0" class="flex flex-col items-center py-20">
        <div class="w-16 h-16 rounded-full bg-muted flex items-center justify-center mb-4">
          <BookOpen class="w-8 h-8 text-muted-foreground" />
        </div>
        <p class="text-muted-foreground">暂时还没有公开的纪念册</p>
      </div>

      <!-- Waterfall Grid -->
      <div v-else class="columns-1 sm:columns-2 lg:columns-3 xl:columns-4 gap-5 space-y-5">
        <div
          v-for="(album, index) in albums"
          :key="album.id"
          class="break-inside-avoid group relative rounded-xl border bg-card overflow-hidden shadow-sm hover:shadow-lg transition-all duration-300 cursor-pointer"
          @click="handleAlbumClick(album.id)"
        >
          <div class="relative overflow-hidden" :class="randomHeight(index)">
            <div class="absolute inset-0 bg-gradient-to-br" :class="randomGradient(index)" />
            <div class="absolute inset-0 flex items-center justify-center">
              <BookOpen class="w-12 h-12 text-primary/15 group-hover:text-primary/25 transition-colors" />
            </div>
            <div class="absolute inset-0 bg-primary/0 group-hover:bg-primary/5 transition-colors" />
          </div>
          <div class="p-4">
            <h3 class="font-serif font-bold text-base text-foreground line-clamp-1 group-hover:text-primary transition-colors">
              {{ album.name }}
            </h3>
            <p class="text-sm text-muted-foreground line-clamp-2 mt-1.5">
              {{ album.des || '一本精美的纪念册' }}
            </p>
            <div class="flex items-center justify-between mt-3 text-xs text-muted-foreground">
              <span v-if="album.userName">by {{ album.userName }}</span>
              <span v-else>&nbsp;</span>
              <span>{{ formatDate(album.createAt) }}</span>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- CTA Banner -->
    <section class="border-t bg-gradient-to-r from-primary/5 via-primary/10 to-accent/10">
      <div class="container mx-auto max-w-7xl px-6 py-20 flex flex-col items-center text-center">
        <Heart class="w-10 h-10 text-primary/30 mb-4" />
        <h2 class="text-3xl md:text-4xl font-serif font-bold text-foreground">
          别让青春只留在记忆里
        </h2>
        <p class="mt-4 text-muted-foreground max-w-lg mx-auto text-lg">
          现在就开始，和同学们一起制作属于你们的纪念册。
        </p>
        <Button size="lg" class="mt-8 gap-2 shadow-lg text-base px-10" @click="router.push(userStore.isLoggedIn ? '/dashboard' : '/register')">
          免费开始
          <ArrowRight class="w-4 h-4" />
        </Button>
      </div>
    </section>

    <!-- Footer -->
    <footer class="border-t bg-card/50">
      <div class="container mx-auto max-w-7xl px-6 py-10">
        <div class="grid grid-cols-1 md:grid-cols-3 gap-8 mb-8">
          <div>
            <div class="flex items-center gap-2 font-serif text-xl font-bold text-foreground mb-3">
              <Book class="w-5 h-5 text-primary" />
              YearArk
            </div>
            <p class="text-sm text-muted-foreground leading-relaxed">
              用 AI 技术让每一本纪念册都独一无二，<br />留住你最珍贵的青春回忆。
            </p>
          </div>
          <div>
            <h4 class="font-medium text-foreground mb-3">快速链接</h4>
            <ul class="space-y-2 text-sm text-muted-foreground">
              <li><a href="#" @click.prevent="scrollToAlbums" class="hover:text-primary transition-colors">精选作品</a></li>
              <li><router-link to="/login" class="hover:text-primary transition-colors">登录</router-link></li>
              <li><router-link to="/register" class="hover:text-primary transition-colors">注册</router-link></li>
            </ul>
          </div>
          <div>
            <h4 class="font-medium text-foreground mb-3">联系我们</h4>
            <ul class="space-y-2 text-sm text-muted-foreground">
              <li>邮箱：[email]</li>
              <li>微信公众号：YearArk</li>
            </ul>
          </div>
        </div>
        <div class="border-t pt-6 flex flex-col md:flex-row items-center justify-between text-xs text-muted-foreground">
          <p>© 2026 YearArk. All rights reserved.</p>
          <p class="mt-2 md:mt-0">留住青春记忆 ❤️</p>
        </div>
      </div>
    </footer>
  </div>
</template>
