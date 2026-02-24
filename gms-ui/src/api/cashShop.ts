import axios from 'axios';
import { cashShopState } from '@/store/modules/cashShop/type';

export interface conditionState {
  id: number;
  subId: number;
  onSale?: number;
  pageNo: number;
  pageSize: number;
  itemId?: number;
  isDbItem?: boolean;
}

export interface cashShopFormState {
  sn: number;
  itemId: number;
  count?: number;
  price?: number;
  priority?: number;
  period?: number;
  commodityGender?: number;
  onSale?: number;
  clz?: number;
  pbCash?: number;
  pbPoint?: number;
  pbGift?: number;
}

export interface addCashShopItemState {
  sn?: number;
  itemId: number;
  categoryId?: number;
  subcategoryId?: number;
  count?: number;
  price?: number;
  priority?: number;
  period?: number;
  commodityGender?: number;
  onSale?: number;
  clz?: number;
  pbCash?: number;
  pbPoint?: number;
  pbGift?: number;
  forceAdd?: boolean;
}

export interface addCashShopItemRtnState {
  exists: boolean;
  itemId: number;
  itemName: string;
  existingCategories: string[];
  newSn: number | null;
}

export interface itemInfoState {
  itemId: number;
  itemName: string;
  valid: boolean;
}

export interface batchFormState {
  data: cashShopState[];
  type: string;
  value?: number;
}

export function getAllCategoryList() {
  return axios.get('/cashShop/v1/getAllCategoryList');
}

export function getCommodityByCategory(condition: conditionState) {
  return axios.post('/cashShop/v1/getCommodityByCategory', condition);
}

export function onSale(data: cashShopFormState) {
  return axios.post('/cashShop/v1/onSale', data);
}

export function offSale(data: cashShopFormState) {
  return axios.post('/cashShop/v1/offSale', data);
}

export function batchOnSale(data: batchFormState) {
  return axios.post('/cashShop/v1/batchOnSale', data);
}

export function getItemInfoById(itemId: number) {
  return axios.get(`/cashShop/v1/getItemInfoById/${itemId}`);
}

export function addCashShopItem(data: addCashShopItemState) {
  return axios.post('/cashShop/v1/addCashShopItem', data);
}

export function updateCashShopItem(data: addCashShopItemState) {
  return axios.post('/cashShop/v1/updateCashShopItem', data);
}

export function deleteCashShopItem(sn: number) {
  return axios.post('/cashShop/v1/deleteCashShopItem', sn);
}

export function batchDeleteCashShopItem(sns: number[]) {
  return axios.post('/cashShop/v1/batchDeleteCashShopItem', sns);
}
