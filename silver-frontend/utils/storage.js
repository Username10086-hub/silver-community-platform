// utils/storage.js
// 统一封装存储：H5 用 sessionStorage（关闭网页即清空），其他平台用 uni 存储

const isH5 = typeof window !== 'undefined' && !!window.sessionStorage;

export const setUser = (user) => {
  if (isH5) {
    sessionStorage.setItem('userInfo', JSON.stringify(user));
  } else {
    uni.setStorageSync('userInfo', user);
  }
};

export const getUser = () => {
  if (isH5) {
    const s = sessionStorage.getItem('userInfo');
    return s ? JSON.parse(s) : null;
  }
  return uni.getStorageSync('userInfo') || null;
};

export const clearUser = () => {
  if (isH5) {
    sessionStorage.removeItem('userInfo');
  } else {
    uni.removeStorageSync('userInfo');
  }
};