<template>
  <a-modal
    v-model:visible="visible"
    :ok-loading="loading"
    :on-before-ok="handleBeforeOk"
    @cancel="handleCancel"
  >
    <template #title> {{ isAddMode ? '添加商品' : '编辑商品' }} </template>
    <div>
      <a-form ref="formRef" :model="formData" :rules="formRules">
        <a-form-item v-if="!isAddMode" label="sn">
          {{ formData.sn }}
        </a-form-item>
        <a-form-item v-if="isAddMode" label="物品ID" field="itemId">
          <a-input-number
            v-model="addFormData.itemId"
            placeholder="请输入物品ID"
            style="width: 100%"
            @change="handleItemIdChange"
          />
        </a-form-item>
        <a-form-item v-if="isAddMode" label="一级分类" field="categoryId">
          <a-select
            v-model="addFormData.categoryId"
            placeholder="请选择一级分类"
          >
            <a-option
              v-for="category in topCategoryList"
              :key="category.id"
              :value="category.id"
            >
              {{ category.name }}
            </a-option>
          </a-select>
        </a-form-item>
        <a-form-item v-if="isAddMode" label="二级分类" field="subcategoryId">
          <a-select
            v-model="addFormData.subcategoryId"
            placeholder="请选择二级分类"
          >
            <a-option
              v-for="category in subCategoryList"
              :key="category.subId"
              :value="category.subId"
            >
              {{ category.subName }}
            </a-option>
          </a-select>
        </a-form-item>
        <a-form-item v-if="itemInfo.itemName" label="物品信息">
          <a-space>
            <span>{{ itemInfo.itemName }}</span>
            <img :src="getIconUrl('item', itemInfo.itemId)" alt="" />
          </a-space>
        </a-form-item>
        <a-form-item
          v-if="existingItemInfo && existingItemInfo.exists"
          label="重复商品"
        >
          <a-alert type="warning">
            <template #title> 该物品已存在 </template>
            <div>
              物品 <b>{{ existingItemInfo.itemName }}</b> (ID:
              {{ existingItemInfo.itemId }}) 已在以下分类中存在：
              <a-tag
                v-for="(cat, index) in existingItemInfo.existingCategories"
                :key="index"
                color="orange"
              >
                {{ cat }}
              </a-tag>
            </div>
            <div style="margin-top: 8px">
              <a-checkbox v-model="addFormData.forceAdd">
                仍然添加到当前分类
              </a-checkbox>
            </div>
          </a-alert>
        </a-form-item>
        <a-form-item v-else-if="!isAddMode" label="物品">
          <a-space>
            {{ formData.itemId }}
            <img :src="getIconUrl('item', formData.itemId)" alt="" />
          </a-space>
        </a-form-item>
        <a-form-item label="数量" field="count">
          <a-input-number v-model="formData.count" />
          <template v-if="tempData.defaultCount" #extra>
            wz默认值 {{ tempData.defaultCount }}
          </template>
        </a-form-item>
        <a-form-item label="价格" field="price">
          <a-input-number v-model="formData.price" />
          <template v-if="tempData.defaultPrice" #extra>
            wz默认值 {{ tempData.defaultPrice }}
          </template>
        </a-form-item>
        <a-form-item label="优先级" field="priority">
          <a-input-number v-model="formData.priority" />
          <template v-if="tempData.defaultPriority" #extra>
            wz默认值 {{ tempData.defaultPriority }}
          </template>
        </a-form-item>
        <a-form-item label="有效期" field="period">
          <a-input-number v-model="formData.period" />
          <template v-if="tempData.defaultPeriod" #extra>
            wz默认值 {{ tempData.defaultPeriod }}
          </template>
        </a-form-item>
        <a-form-item label="状态">
          <a-switch
            v-model="formData.onSale"
            type="round"
            :checked-value="1"
            :unchecked-value="0"
          >
            <template #checked> 上架中 </template>
            <template #unchecked> 待售 </template>
          </a-switch>
          <template #extra>
            wz默认值 {{ tempData.defaultOnSale ? '上架中' : '待售' }}
          </template>
        </a-form-item>

        <a-form-item label="性别">
          <a-select v-model="formData.commodityGender">
            <a-option :value="0">男</a-option>
            <a-option :value="1">女</a-option>
            <a-option :value="2">通用</a-option>
          </a-select>
          <template #extra>
            wz默认值
            {{
              tempData.defaultGender === 0
                ? '男'
                : tempData.defaultGender === 1
                ? '女'
                : tempData.defaultGender === 2
                ? '通用'
                : ''
            }}
          </template>
        </a-form-item>
        <a-form-item label="标签">
          <a-select v-model="formData.clz" allow-clear>
            <a-option :value="0">NEW</a-option>
            <a-option :value="1">SALE</a-option>
            <a-option :value="2">HOT</a-option>
            <a-option :value="3">EVENT</a-option>
          </a-select>
          <template v-if="tempData.defaultClz" #extra>
            wz默认值
            {{
              tempData.defaultClz === 0
                ? 'NEW'
                : tempData.defaultClz === 1
                ? 'SALE'
                : tempData.defaultClz === 2
                ? 'HOT'
                : tempData.defaultClz === 3
                ? 'EVENT'
                : ''
            }}
          </template>
        </a-form-item>
        <a-form-item label="pbCash">
          <a-input-number v-model="formData.pbCash" />
          <template v-if="tempData.defaultPBCash" #extra>
            wz默认值 {{ tempData.defaultPBCash }}
          </template>
        </a-form-item>
        <a-form-item label="pbPoint">
          <a-input-number v-model="formData.pbPoint" />
          <template v-if="tempData.defaultPBPoint" #extra>
            wz默认值 {{ tempData.defaultPBPoint }}
          </template>
        </a-form-item>
        <a-form-item label="pbGift">
          <a-input-number v-model="formData.pbGift" />
          <template v-if="tempData.defaultPBGift" #extra>
            wz默认值 {{ tempData.defaultPBGift }}
          </template>
        </a-form-item>
      </a-form>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
  import { ref, watch } from 'vue';
  import { cashShopState } from '@/store/modules/cashShop/type';
  import {
    cashShopFormState,
    offSale,
    onSale,
    getItemInfoById,
    addCashShopItem,
    updateCashShopItem,
    getAllCategoryList,
  } from '@/api/cashShop';
  import useLoading from '@/hooks/loading';
  import { Message } from '@arco-design/web-vue';
  import { getIconUrl } from '@/utils/mapleStoryAPI';
  import { categoryState } from '@/store/modules/cashShop/type';

  const { setLoading, loading } = useLoading(false);
  const visible = ref<boolean>(false);
  const formRef = ref();
  const isAddMode = ref<boolean>(false);
  const formData = ref<cashShopFormState>({ sn: -1, itemId: -1 });
  const tempData = ref<cashShopState>({ sn: -1, itemId: -1 });
  const addFormData = ref({
    itemId: undefined as number | undefined,
    categoryId: undefined as number | undefined,
    subcategoryId: undefined as number | undefined,
    forceAdd: false,
  });
  const itemInfo = ref({
    itemId: -1,
    itemName: '',
    valid: false,
  });
  const existingItemInfo = ref<{
    exists: boolean;
    itemId: number;
    itemName: string;
    existingCategories: string[];
  } | null>(null);
  const allCategoryList = ref<categoryState[]>([]);
  const topCategoryList = ref<categoryState[]>([]);
  const subCategoryList = ref<categoryState[]>([]);

  const props = defineProps<{
    categoryId: number;
    subcategoryId: number;
  }>();
  const emit = defineEmits(['loadData']);

  const formRules = {
    itemId: [
      {
        required: true,
        message: '请输入物品ID',
      },
    ],
    categoryId: [
      {
        required: true,
        message: '请选择一级分类',
      },
    ],
    subcategoryId: [
      {
        required: true,
        message: '请选择二级分类',
      },
    ],
    count: [
      {
        required: true,
        message: '请输入数量',
      },
    ],
    price: [
      {
        required: true,
        message: '请输入价格',
      },
    ],
  };

  const loadCategories = async () => {
    const { data } = await getAllCategoryList();
    allCategoryList.value = data;
    const tc = topCategoryList.value;
    tc.length = 0;
    data.forEach((_data: any) => {
      if (_data.id === 8) return;
      let exist = false;
      for (let tci = 0; tci < tc.length; tci += 1) {
        if (tc[tci].id === _data.id) {
          exist = true;
          break;
        }
      }
      if (!exist) {
        tc.push(_data);
      }
    });
  };

  watch(
    () => addFormData.value.categoryId,
    (newVal) => {
      subCategoryList.value = allCategoryList.value.filter((_data) => {
        return _data.id === newVal;
      });
      if (
        !subCategoryList.value.find(
          (c) => c.subId === addFormData.value.subcategoryId
        )
      ) {
        addFormData.value.subcategoryId = undefined;
      }
    }
  );

  const handleItemIdChange = async (value: number | undefined) => {
    if (!value) {
      itemInfo.value = { itemId: -1, itemName: '', valid: false };
      return;
    }
    try {
      const { data } = await getItemInfoById(value);
      if (data.valid) {
        itemInfo.value = data;
      } else {
        Message.error('无效的物品ID');
        itemInfo.value = { itemId: -1, itemName: '', valid: false };
      }
    } catch (error) {
      Message.error('获取物品信息失败');
      itemInfo.value = { itemId: -1, itemName: '', valid: false };
    }
  };

  const handleBeforeOk = async () => {
    if (isAddMode.value) {
      try {
        await formRef.value.validate();
      } catch (error) {
        return false;
      }
      if (!itemInfo.value.valid) {
        Message.error('请输入有效的物品ID');
        return false;
      }
    }
    setLoading(true);
    try {
      if (isAddMode.value) {
        const { data } = await addCashShopItem({
          itemId: addFormData.value.itemId!,
          categoryId: addFormData.value.categoryId!,
          subcategoryId: addFormData.value.subcategoryId!,
          count: formData.value.count,
          price: formData.value.price,
          priority: formData.value.priority,
          period: formData.value.period,
          commodityGender: formData.value.commodityGender,
          onSale: formData.value.onSale,
          clz: formData.value.clz,
          pbCash: formData.value.pbCash,
          pbPoint: formData.value.pbPoint,
          pbGift: formData.value.pbGift,
          forceAdd: addFormData.value.forceAdd,
        });
        if (data.exists && !addFormData.value.forceAdd) {
          existingItemInfo.value = data;
          Message.warning('该物品已存在，请确认是否仍然添加');
          return false;
        }
        Message.success('添加成功！');
      } else {
        await updateCashShopItem({
          sn: formData.value.sn,
          itemId: formData.value.itemId,
          count: formData.value.count,
          price: formData.value.price,
          priority: formData.value.priority,
          period: formData.value.period,
          commodityGender: formData.value.commodityGender,
          onSale: formData.value.onSale,
          clz: formData.value.clz,
          pbCash: formData.value.pbCash,
          pbPoint: formData.value.pbPoint,
          pbGift: formData.value.pbGift,
        });
        Message.success('更新成功！');
      }
      visible.value = false;
      emit('loadData');
      return true;
    } catch (error) {
      Message.error(isAddMode.value ? '添加失败' : '更新失败');
      return false;
    } finally {
      setLoading(false);
    }
  };

  const handleCancel = () => {
    visible.value = false;
  };

  const initForm = (data: cashShopState) => {
    isAddMode.value = false;
    tempData.value = data;
    formData.value = {
      sn: data.sn,
      itemId: data.itemId,
      count: data.count,
      price: data.price,
      priority: data.priority,
      period: data.period,
      commodityGender: data.gender,
      onSale: data.onSale ? 1 : 0,
      clz: data.clz,
      pbCash: data.pbCash,
      pbPoint: data.pbPoint,
      pbGift: data.pbGift,
    };
    itemInfo.value = {
      itemId: data.itemId,
      itemName: data.itemName || '',
      valid: true,
    };
    existingItemInfo.value = null;
    visible.value = true;
  };

  const initAddForm = async () => {
    isAddMode.value = true;
    await loadCategories();
    addFormData.value = {
      itemId: undefined,
      categoryId: props.categoryId,
      subcategoryId: props.subcategoryId,
      forceAdd: false,
    };
    subCategoryList.value = allCategoryList.value.filter((_data) => {
      return _data.id === props.categoryId;
    });
    itemInfo.value = {
      itemId: -1,
      itemName: '',
      valid: false,
    };
    existingItemInfo.value = null;
    formData.value = {
      sn: -1,
      itemId: -1,
      count: 1,
      price: 0,
      priority: 0,
      period: 0,
      commodityGender: 2,
      onSale: 0,
      clz: undefined,
      pbCash: 0,
      pbPoint: 0,
      pbGift: 0,
    };
    tempData.value = { sn: -1, itemId: -1 };
    visible.value = true;
  };

  defineExpose({ initForm, initAddForm });
</script>

<script lang="ts">
  export default {
    name: 'CashShopForm',
  };
</script>
