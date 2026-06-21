<template>
  <div class="chat-container">
    <!-- 消息列表 -->
    <div class="message-list" ref="messageListRef">
      <div v-for="(msg, index) in messages" :key="index"
           :class="['message', msg.role === 'user' ? 'message-user' : 'message-ai']">
        <div class="message-avatar">
          <van-image v-if="msg.role === 'user'" round width="32" height="32"
                     :src="user?.avatarUrl || 'https://fastly.jsdelivr.net/npm/@vant/assets/cat.jpeg'" />
          <van-icon v-else name="chat" size="32" color="#1989fa" />
        </div>
        <div class="message-bubble">
          <div class="message-text" v-html="formatMessage(msg.content)"></div>
        </div>
      </div>
      <!-- 加载中 -->
      <div v-if="loading" class="message message-ai">
        <div class="message-avatar">
          <van-icon name="chat" size="32" color="#1989fa" />
        </div>
        <div class="message-bubble">
          <van-loading size="16" />
        </div>
      </div>
    </div>

    <!-- 输入框 -->
    <div class="input-bar">
      <van-field v-model="inputText" placeholder="输入你的问题..." @keyup.enter="sendMessage"
                 :disabled="loading" />
      <van-button type="primary" size="small" @click="sendMessage" :disabled="loading || !inputText.trim()">
        发送
      </van-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import {ref, onMounted, nextTick} from 'vue';
import {useRoute} from 'vue-router';
import {getCurrentUser} from '../services/user';
import type {UserType} from '../models/user';

interface ChatMessage {
  role: 'user' | 'ai';
  content: string;
}

const user = ref<UserType | null>(null);
const messages = ref<ChatMessage[]>([]);
const inputText = ref('');
const loading = ref(false);
const messageListRef = ref<HTMLElement>();
const route = useRoute();

// 滚动到底部
const scrollToBottom = () => {
  nextTick(() => {
    if (messageListRef.value) {
      messageListRef.value.scrollTop = messageListRef.value.scrollHeight;
    }
  });
};

// 格式化消息（简单 markdown）
const formatMessage = (text: string) => {
  return text
      .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
      .replace(/`(.*?)`/g, '<code>$1</code>')
      .replace(/\n/g, '<br>');
};

// 发送消息并接收 SSE 流式响应
// silent=true 时不显示用户消息（用于隐式触发问候）
const sendAndStream = async (message: string, silent = false) => {
  loading.value = true;
  if (!silent) {
    messages.value.push({role: 'user', content: message});
  }
  messages.value.push({role: 'ai', content: ''});
  scrollToBottom();

  try {
    const baseUrl = import.meta.env.DEV ? 'http://localhost:8080/api' : '';
    const url = `${baseUrl}/ai/chat?message=${encodeURIComponent(message)}`;

    const response = await fetch(url, {credentials: 'include'});

    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`);
    }

    const reader = response.body!.getReader();
    const decoder = new TextDecoder();
    let aiMessage = '';

    while (true) {
      const {done, value} = await reader.read();
      if (done) break;

      const chunk = decoder.decode(value);
      const lines = chunk.split('\n');

      for (const line of lines) {
        if (line.startsWith('data:')) {
          aiMessage += line.slice(5);
          messages.value[messages.value.length - 1].content = aiMessage;
        }
      }
      scrollToBottom();
    }
  } catch (e: any) {
    messages.value[messages.value.length - 1].content = '请求失败：' + (e.message || '网络错误');
  } finally {
    loading.value = false;
  }
};

// 用户手动发送
const sendMessage = () => {
  const text = inputText.value.trim();
  if (!text || loading.value) return;
  inputText.value = '';
  sendAndStream(text);
};

// 打开组件时隐式发送问候（后端会自动注入 tags 上下文）
onMounted(async () => {
  user.value = await getCurrentUser();
  // 仅当路由显式携带 silentGreet=1 时，才触发隐式问候
  if (route.query.silentGreet === '1') {
    sendAndStream('你好，请简短介绍一下你能帮我什么', true);
  }
});
</script>

<style scoped>
.chat-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #f5f5f5;
}

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
}

.message {
  display: flex;
  margin-bottom: 12px;
  gap: 8px;
}

.message-user {
  flex-direction: row-reverse;
}

.message-bubble {
  max-width: 75%;
  padding: 10px 14px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;
}

.message-user .message-bubble {
  background: #1989fa;
  color: white;
}

.message-ai .message-bubble {
  background: white;
  color: #333;
}

.message-ai .message-bubble :deep(code) {
  background: #f0f0f0;
  padding: 1px 4px;
  border-radius: 3px;
  font-size: 13px;
}

.input-bar {
  display: flex;
  align-items: center;
  padding: 8px 12px;
  background: white;
  border-top: 1px solid #eee;
  gap: 8px;
}

.input-bar .van-field {
  flex: 1;
}

.message-avatar {
  flex-shrink: 0;
}
</style>
