<template>
  <view class="register-wrap">
    <!-- ========== 第一步：选择角色 ========== -->
    <view v-if="step === 1" class="role-select">
      <view class="page-title">请选择您的身份</view>
      <view class="role-list">
        <view class="role-card" @click="chooseRole(1)">
          <text class="icon">👴👵</text>
          <text class="label">老人</text>
          <text class="desc">需要被照顾的老年人</text>
        </view>
        <view class="role-card" @click="chooseRole(4)">
          <text class="icon">👨👩</text>
          <text class="label">子女（监护人）</text>
          <text class="desc">照顾父母的子女</text>
        </view>
        <view class="role-card" @click="chooseRole(2)">
          <text class="icon">🤝</text>
          <text class="label">志愿者</text>
          <text class="desc">提供公益服务的志愿者</text>
        </view>
      </view>
      <navigator url="/pages/login/login" class="to-login">已有账号？去登录</navigator>
    </view>

    <!-- ========== 第二步：填表 ========== -->
    <view v-if="step === 2">
      <view class="step-header">
        <text class="role-name">{{ roleName }}注册</text>
      </view>

      <view class="card">
        <!-- 通用字段 -->
        <view class="input-group">
          <label>用户名（必填）</label>
          <input v-model="form.username" placeholder="请输入用户名" />
        </view>
        <view class="input-group">
          <label>密码（至少6位）</label>
          <input v-model="form.password" type="password" placeholder="请输入密码" />
        </view>
        <view class="input-group">
          <label>真实姓名</label>
          <input v-model="form.realName" placeholder="请输入真实姓名" />
        </view>
        <view class="input-group">
          <label>年龄</label>
          <input v-model="form.age" type="number" placeholder="请输入年龄" />
        </view>
        <view class="input-group">
          <label>性别</label>
          <radio-group class="radio-group" @change="onGenderChange">
            <label><radio value="1" :checked="form.gender === 1" color="#4A90D9" />男</label>
            <label><radio value="2" :checked="form.gender === 2" color="#4A90D9" />女</label>
          </radio-group>
        </view>
        <view class="input-group">
          <label>手机号</label>
          <input v-model="form.phone" placeholder="请输入手机号" />
        </view>

        <!-- 老人专属 -->
        <template v-if="form.role === 1">
          <view class="section-title">🏠 家庭信息</view>
          <view class="input-group">
            <label>家庭住址</label>
            <input v-model="form.address" placeholder="请输入家庭住址" />
          </view>

          <view class="section-title">📋 养老基础档案</view>
          <view class="input-group">
            <label>居住状态</label>
            <radio-group class="radio-group" @change="onLivingChange">
              <label><radio value="1" :checked="form.livingStatus === 1" color="#4A90D9" />独居</label>
              <label><radio value="2" :checked="form.livingStatus === 2" color="#4A90D9" />与子女同住</label>
              <label><radio value="3" :checked="form.livingStatus === 3" color="#4A90D9" />夫妻同住</label>
            </radio-group>
          </view>
          <view class="input-group">
            <label>行动能力</label>
            <radio-group class="radio-group" @change="onMobilityChange">
              <label><radio value="1" :checked="form.mobility === 1" color="#4A90D9" />自理</label>
              <label><radio value="2" :checked="form.mobility === 2" color="#4A90D9" />半自理</label>
              <label><radio value="3" :checked="form.mobility === 3" color="#4A90D9" />失能</label>
            </radio-group>
          </view>
          <view class="input-group">
            <label>慢性病史</label>
            <textarea v-model="form.chronicDisease" placeholder="如：高血压、糖尿病（没有可留空）" />
          </view>
          <view class="input-group">
            <label>饮食禁忌</label>
            <textarea v-model="form.dietaryRestriction" placeholder="如：忌辛辣、忌海鲜（没有可留空）" />
          </view>
          <view class="input-group">
            <label>紧急联系人电话</label>
            <input v-model="form.emergencyPhone" placeholder="请输入紧急联系电话" />
          </view>

          <!-- 老人自注册时，选择是否绑定子女 -->
          <template v-if="!form.currentGuardianId">
            <view class="section-title">👨‍👧 是否绑定子女</view>
            <radio-group class="radio-group" @change="onWantBindChange">
              <label><radio value="0" :checked="form.wantBind === 0" color="#4A90D9" />暂不绑定</label>
              <label><radio value="1" :checked="form.wantBind === 1" color="#4A90D9" />绑定子女</label>
            </radio-group>

            <view v-if="form.wantBind === 1" class="binding-list">
              <view v-for="(b, i) in form.guardianBindings" :key="i" class="binding-item">
                <input v-model="b.guardianName" placeholder="子女姓名" />
                <input v-model="b.guardianId" placeholder="子女ID" type="number" />
                <text class="remove" @click="removeBinding(i)">✕</text>
              </view>
              <view class="add-row" @click="addBinding">＋ 添加一个子女</view>
            </view>
          </template>
        </template>

        <button class="btn-submit" @click="handleRegister">立即注册</button>
      </view>
    </view>
  </view>
</template>

<script>
import { request } from '@/utils/request.js'
import { getUser } from '@/utils/storage.js'

export default {
  data() {
    return {
      step: 1,
      form: {
        username: '',
        password: '',
        realName: '',
        age: '',
        gender: 1,
        phone: '',
        role: null,
        address: '',
        livingStatus: 1,
        mobility: 1,
        chronicDisease: '',
        dietaryRestriction: '',
        emergencyPhone: '',
        wantBind: 0,
        guardianBindings: [{ guardianName: '', guardianId: '' }],
        currentGuardianId: null
      }
    }
  },
  computed: {
    roleName() {
      return { 1: '老人', 2: '志愿者', 4: '子女' }[this.form.role] || ''
    }
  },
  onLoad(options) {
    if (options && options.fromGuardian === '1') {
      const user = getUser()
      if (user && user.role === 4) {
        this.form.role = 1
        this.form.currentGuardianId = user.id
        this.step = 2
      }
    }
  },
  methods: {
    chooseRole(role) {
      this.form.role = role
      this.step = 2
    },
    // ===== 所有 radio 事件改为方法 =====
    onGenderChange(e) {
      this.form.gender = Number(e.detail.value)
    },
    onLivingChange(e) {
      this.form.livingStatus = Number(e.detail.value)
    },
    onMobilityChange(e) {
      this.form.mobility = Number(e.detail.value)
    },
    onWantBindChange(e) {
      this.form.wantBind = Number(e.detail.value)
    },
    // ===== 绑定列表操作 =====
    addBinding() {
      this.form.guardianBindings.push({ guardianName: '', guardianId: '' })
    },
    removeBinding(i) {
      this.form.guardianBindings.splice(i, 1)
    },
    // ===== 提交注册 =====
    async handleRegister() {
      const f = this.form
      if (!f.username || !f.password || !f.realName || !f.phone) {
        return uni.showToast({ title: '请完善必填项', icon: 'none' })
      }
      if (f.password.length < 6) {
        return uni.showToast({ title: '密码至少6位', icon: 'none' })
      }
      if (f.role === 1 && !f.address) {
        return uni.showToast({ title: '请填写家庭住址', icon: 'none' })
      }

      const payload = {
        username: f.username,
        password: f.password,
        realName: f.realName,
        phone: f.phone,
        role: f.role,
        age: f.age ? Number(f.age) : null,
        gender: Number(f.gender)
      }
      if (f.role === 1) {
        Object.assign(payload, {
          address: f.address,
          livingStatus: Number(f.livingStatus),
          mobility: Number(f.mobility),
          chronicDisease: f.chronicDisease,
          dietaryRestriction: f.dietaryRestriction,
          emergencyPhone: f.emergencyPhone,
          currentGuardianId: f.currentGuardianId
        })
        if (f.wantBind === 1 && !f.currentGuardianId) {
          payload.guardianBindings = f.guardianBindings
            .filter(b => b.guardianId && b.guardianName)
            .map(b => ({ guardianId: Number(b.guardianId), guardianName: b.guardianName }))
        }
      }

      try {
        await request({
          url: '/user/register',
          method: 'POST',
          data: payload,
          header: { 'Content-Type': 'application/json' }
        })
        uni.showToast({ title: '注册成功！' })
        setTimeout(() => {
          if (f.currentGuardianId) {
            uni.navigateBack()
          } else {
            uni.reLaunch({ url: '/pages/login/login' })
          }
        }, 1000)
      } catch (e) {
        console.log('注册失败', e)
      }
    }
  }
}
</script>

<style scoped>
.register-wrap { padding: 30rpx; }

.role-select { padding-top: 60rpx; }
.page-title { font-size: 44rpx; font-weight: 700; text-align: center; margin-bottom: 60rpx; color: #1a2332; }
.role-list { display: flex; flex-direction: column; gap: 30rpx; }
.role-card {
  background: #fff; border-radius: 24rpx; padding: 40rpx;
  display: flex; flex-direction: column; align-items: center;
  box-shadow: 0 4rpx 20rpx rgba(0,0,0,0.05);
}
.role-card .icon { font-size: 80rpx; margin-bottom: 16rpx; }
.role-card .label { font-size: 36rpx; font-weight: 600; color: #1a2332; }
.role-card .desc { font-size: 26rpx; color: #8A9AAE; margin-top: 8rpx; }
.to-login { text-align: center; display: block; margin-top: 40rpx; color: #4A90D9; font-size: 28rpx; }

.step-header { display: flex; align-items: center; margin-bottom: 24rpx; }
.role-name { font-size: 34rpx; font-weight: 600; color: #1a2332; }

.card { background: #fff; border-radius: 24rpx; padding: 30rpx; }
.input-group { margin-bottom: 28rpx; }
.input-group label { display: block; font-size: 28rpx; color: #4a5a6e; margin-bottom: 10rpx; }
/* 用 uni-input / uni-textarea 选择器，命中外层组件 */
.input-group uni-input,
.input-group uni-textarea {
  width: 100%;
  background: #f7f9fc;
  border-radius: 16rpx;
  padding: 20rpx 24rpx;
  border: 1px solid #e6edf4;
  font-size: 28rpx;
  box-sizing: border-box;
  display: block;
  min-height: 80rpx;    /* 关键：给外层一个最小高度，防止塌陷 */
}
.section-title { font-size: 30rpx; font-weight: 600; color: #2d3a4a; margin: 30rpx 0 20rpx; }
.radio-group { display: flex; gap: 30rpx; flex-wrap: wrap; font-size: 28rpx; color: #4a5a6e; }

.binding-list { margin-top: 20rpx; }
.binding-item { display: flex; gap: 16rpx; margin-bottom: 16rpx; align-items: center; }
.binding-item input { flex: 1; }
.binding-item .remove { color: #ff4d4f; font-size: 36rpx; padding: 0 16rpx; }
.add-row { text-align: center; color: #4A90D9; font-size: 28rpx; padding: 20rpx 0; }

.btn-submit {
  margin-top: 30rpx; width: 100%; background: #4A90D9; color: #fff;
  border-radius: 50rpx; padding: 24rpx; font-size: 32rpx; border: none;
}
</style>