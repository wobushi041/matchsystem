<template>
  <van-form @submit="onSubmit">
      <van-field
          v-model="editUser.currentValue"
          :name="editUser.editKey"
          :label="editUser.editName"
          :placeholder="`请输入${editUser.editName}`"
      />
    <div style="margin: 16px;">
      <van-button round block type="primary" native-type="submit">
        提交
      </van-button>
    </div>
  </van-form>
</template>

<script setup lang="ts">
import {useRoute, useRouter} from "vue-router";
import { ref } from "vue";
import myAxios from "../plugins/myAxios";
import {Toast} from "vant";
import {getCurrentUser} from "../services/user";

const route = useRoute();
const router = useRouter();

const getGenderText = (value: unknown) => {
  if (value === 1 || value === '1') {
    return '男';
  }
  if (value === 2 || value === '2') {
    return '女';
  }
  return value;
}

const getGenderValue = (value: unknown) => {
  if (value === '男' || value === 1 || value === '1') {
    return 1;
  }
  if (value === '女' || value === 2 || value === '2') {
    return 2;
  }
  return value;
}

const editUser = ref({
  editKey: route.query.editKey,
  currentValue: route.query.editKey === 'gender'
    ? getGenderText(route.query.currentValue)
    : route.query.currentValue,
  editName: route.query.editName,
})

const onSubmit = async () => {
  const currentUser = await getCurrentUser();

  if (!currentUser) {
    Toast.fail('用户未登录');
    return;
  }

  console.log(currentUser, '当前用户')

  const submitValue = editUser.value.editKey === 'gender'
    ? getGenderValue(editUser.value.currentValue)
    : editUser.value.currentValue;

  const res = await myAxios.post('/user/update', {
    'id': currentUser.id,
    [editUser.value.editKey as string]: submitValue,
  })
  console.log(res, '更新请求');
  if (res.code === 0 && res.data > 0) {
    Toast.success('修改成功');
    router.back();
  } else {
    Toast.fail('修改错误');
  }
};

</script>

<style scoped>

</style>
