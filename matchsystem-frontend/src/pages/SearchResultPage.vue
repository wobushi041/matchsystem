<template>
  <user-card-list :user-list="userList" :loading="loading" />
  <van-empty v-if="!userList || userList.length < 1" description="搜索结果为空" />
</template>

<script setup lang="ts">
import {onMounted, ref} from 'vue';
import {useRoute} from "vue-router";
import myAxios from "../plugins/myAxios";
import {Toast} from "vant";
import qs from 'qs';
import UserCardList from "../components/UserCardList.vue";

const route = useRoute();
const {tags} = route.query;

const userList = ref([]);
const loading = ref(true);

const normalizeTags = (rawTags) => {
  if (!rawTags) {
    return [];
  }
  if (Array.isArray(rawTags)) {
    return rawTags;
  }
  try {
    const parsedTags = JSON.parse(rawTags);
    return Array.isArray(parsedTags) ? parsedTags : [String(parsedTags)];
  } catch (error) {
    return String(rawTags)
      .split(/[,，]/)
      .map(tag => tag.trim())
      .filter(Boolean);
  }
};

onMounted(async () => {
  loading.value = true;
  const userListData = await myAxios.get('/user/search/tags', {
    params: {
      tagNameList: tags
    },
    paramsSerializer: params => {
      return qs.stringify(params, {indices: false})
    }
  })
      .then(function (response) {
        console.log('/user/search/tags succeed', response);
        return response?.data;
      })
      .catch(function (error) {
        console.error('/user/search/tags error', error);
        Toast.fail('请求失败');
      })
  console.log(userListData)
  if (userListData) {
    userListData.forEach(user => {
      user.tags = normalizeTags(user.tags);
    })
    userList.value = userListData;
  }
  loading.value = false;
})



</script>

<style scoped>

</style>
