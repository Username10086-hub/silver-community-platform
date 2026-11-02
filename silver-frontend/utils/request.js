// utils/request.js
const BASE_URL = 'http://localhost:8080/api' // 网关地址

// 封装 uni.request 为 Promise 风格
export const request = (options) => {
    return new Promise((resolve, reject) => {
        uni.request({
            url: BASE_URL + options.url,
            method: options.method || 'GET',
            data: options.data || {},
            header: {
                'Content-Type': 'application/x-www-form-urlencoded', // 适配后端 @RequestParam
                ...options.header
            },
            success: (res) => {
                // 后端统一返回格式 { code: 200, msg, data }
                if (res.data.code === 200) {
                    resolve(res.data.data)
                } else {
                    uni.showToast({ title: res.data.msg || '请求失败', icon: 'none' })
                    reject(res.data)
                }
            },
            fail: (err) => {
                uni.showToast({ title: '网络异常', icon: 'none' })
                reject(err)
            }
        })
    })
}