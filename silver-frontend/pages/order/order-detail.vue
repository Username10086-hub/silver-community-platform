<template>
  <view class="page-container" v-if="order">
    <view class="card">
      <view class="header">
        <text class="title">{{ ['陪护','助老帮扶','社区帮扶'][order.serviceType-1] }}</text>
        <text class="tag" :style="{background: statusBg}">{{ statusText }}</text>
      </view>
      <view class="info">👤 老人：{{ elderName }}</view>
      <view class="info">📍 地址：{{ order.address || '未填' }}</view>
      <view class="info" v-if="order.contactPhone">📞 电话：{{ order.contactPhone }}</view>
      <view class="info">👥 进度：{{ order.acceptedCount }}/{{ order.peopleCount }} 人已接单</view>
      <view class="info" v-if="order.appointmentTime">⏰ 预约：{{ order.appointmentTime }}</view>
      <view class="info">📝 {{ order.remark || '无' }}</view>
      <view class="info">📅 发布：{{ order.createTime }}</view>

      <view v-if="order.status === 5" class="revoke-banner">
        ⚠️ 该工单已被超级管理员撤销，不可再进行任何操作
      </view>

      <view v-if="order.status === 3 && userInfo.role !== 2 && order.cancelBy" class="cancel-info">
        <view class="reject-label">🚫 取消者</view>
        <view class="reject-content">ID: {{ order.cancelBy }} · {{ roleText(order.cancelByRole) }}</view>
      </view>

      <view v-if="(userInfo.role === 5 || userInfo.role === 3) && order.reviewerId" class="reviewer-info">
        <view class="reject-label">✅ 处理管理员</view>
        <view class="reject-content">ID: {{ order.reviewerId }}</view>
      </view>

      <view v-if="order.auditStatus === 2 && order.rejectReason" class="reject-box">
        <view class="reject-label">❌ 拒绝理由</view>
        <view class="reject-content">{{ order.rejectReason }}</view>
      </view>

      <view v-if="isLockedByOther" class="lock-tip">
        🔒 该工单正在被管理员 ID:{{ order.reviewingAdminId }} 审核中
      </view>
    </view>

    <view class="card">
      <view class="section-title">👥 接单志愿者</view>
      <view v-if="volunteers.length===0" class="empty">暂无志愿者接单</view>
      <view v-for="v in volunteers" :key="v.volunteerId" class="vol-item">
        <view>
          <view class="vol-name">
            {{ v.realName }}
            <text v-if="v.confirmStatus === 1" class="flag-warn">待确认修改</text>
          </view>
          <view class="vol-info">{{ v.phone }} · {{ v.status===2 ? '已完成' : '进行中' }}</view>
        </view>
      </view>
    </view>

    <!-- 已撤销时：不显示任何按钮 -->

    <view v-if="canAccept" class="btn-wrap">
      <button class="btn-accept" @click="acceptOrder">✅ 立即接单</button>
    </view>

    <view v-if="canVolunteerComplete" class="btn-wrap">
      <button class="btn-accept" @click="completeMyPart">✅ 提交完成，等待确认</button>
    </view>

    <view v-if="canVolunteerCancel" class="btn-wrap">
      <button class="btn-cancel" @click="volunteerCancel">取消我的接单</button>
    </view>

    <view v-if="canConfirm" class="btn-wrap">
      <button class="btn-confirm" @click="confirmOrder">👍 确认验收，工单完成</button>
    </view>

    <view v-if="canAudit" class="btn-wrap">
      <button class="btn-accept" @click="auditPass">✅ 通过审核</button>
      <button class="btn-cancel" style="margin-top:20rpx;" @click="showRejectModal = true">❌ 拒绝</button>
    </view>

    <view v-if="canSuperDispatch" class="btn-wrap">
      <button class="btn-confirm" @click="openDispatch">👤 分派给管理员审核</button>
    </view>

    <view v-if="canSuperRevoke" class="btn-wrap">
      <button class="btn-revoke" @click="superRevoke">🗑️ 一键撤销该工单</button>
    </view>

    <view v-if="canEditOrCancelRejected" class="btn-wrap">
      <button class="btn-confirm" @click="openEditModal">✏️ 编辑并重新提交</button>
      <button class="btn-cancel" style="margin-top:20rpx;" @click="cancelOrder">取消工单</button>
    </view>

    <view v-if="canEditOrCancelNormal" class="btn-wrap">
      <button class="btn-confirm" @click="openEditModal">✏️ 修改工单</button>
      <button class="btn-cancel" style="margin-top:20rpx;" @click="cancelOrder">取消工单</button>
    </view>

    <view v-if="showRejectModal" class="modal-mask" @click="showRejectModal=false">
      <view class="modal-box" @click.stop>
        <view class="modal-title">拒绝理由</view>
        <textarea v-model="rejectReason" class="modal-textarea" placeholder="请输入拒绝理由（必填）"></textarea>
        <button class="btn-primary" @click="auditReject">确认拒绝</button>
        <button class="btn-cancel" @click="showRejectModal=false">取消</button>
      </view>
    </view>

    <view v-if="showEditModal" class="modal-mask" @click="showEditModal=false">
      <view class="modal-box" @click.stop>
        <view class="modal-title">编辑工单</view>
        <view class="form-row">
          <text class="label">服务类型</text>
          <picker mode="selector" :range="types" @change="onEditType">
            <view class="picker-value">{{ types[editForm.serviceType - 1] }}</view>
          </picker>
        </view>
        <view class="form-row"><text class="label">地址</text><input v-model="editForm.address" /></view>
        <view class="form-row"><text class="label">电话</text><input v-model="editForm.contactPhone" /></view>
        <view class="form-row"><text class="label">人数</text><input v-model="editForm.peopleCount" type="number" /></view>
        <view class="form-row">
          <text class="label">预约日期</text>
          <picker mode="date" @change="onEditDate">
            <view class="picker-value">{{ editForm.appointmentDate || '选择日期' }}</view>
          </picker>
        </view>
        <view class="form-row">
          <text class="label">预约时间</text>
          <picker mode="time" @change="onEditTime">
            <view class="picker-value">{{ editForm.appointmentTimeOnly || '选择时间' }}</view>
          </picker>
        </view>
        <view class="form-row textarea-row">
          <text class="label">需求</text>
          <textarea v-model="editForm.remark" class="modal-textarea"></textarea>
        </view>
        <button class="btn-primary" @click="saveEdit">保存</button>
        <button class="btn-cancel" @click="showEditModal=false">取消</button>
      </view>
    </view>

    <view v-if="showDispatchModal" class="modal-mask" @click="showDispatchModal=false">
      <view class="modal-box" @click.stop>
        <view class="modal-title">分派给管理员审核</view>
        <view v-for="a in adminList" :key="a.id" class="dispatch-item" @click="dispatchTo(a.id)">
          <view class="dispatch-name">{{ a.realName }}</view>
          <view class="dispatch-desc">ID: {{ a.id }} · {{ a.phone }}</view>
        </view>
        <view v-if="adminList.length===0" class="empty">暂无其他管理员</view>
        <button class="btn-cancel" @click="showDispatchModal=false">取消</button>
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
      orderId: '', order: null, elderName: '', volunteers: [],
      userInfo: {}, myVolunteerRecord: null,
      showRejectModal: false, rejectReason: '',
      showEditModal: false,
      showDispatchModal: false, adminList: [],
      types: ['陪护', '助老帮扶', '社区帮扶'],
      editForm: { serviceType: 1, address: '', contactPhone: '', peopleCount: 1, appointmentDate: '', appointmentTimeOnly: '', remark: '' }
    }
  },
  computed: {
    isRevoked() {
      return this.order && this.order.status === 5
    },
    statusText() {
      if (!this.order) return ''
      if (this.order.status === 5) return '已撤销'
      if (this.order.auditStatus === 0) return this.order.reviewingAdminId ? '审核中' : '待审核'
      if (this.order.auditStatus === 3) return '再审核'
      if (this.order.auditStatus === 2) return '已拒绝'
      return ['待接单','进行中','已完成','已取消','待验收'][this.order.status] || ''
    },
    statusBg() {
      if (!this.order) return '#FFF3E0'
      if (this.order.status === 5) return '#E0E0E0'
      if (this.order.auditStatus === 0) return '#E8F4FD'
      if (this.order.auditStatus === 2) return '#FFE8E8'
      if (this.order.status === 3) return '#F0F0F0'
      return '#FFF3E0'
    },
    isLockedByOther() {
      if (!this.order || !this.userInfo) return false
      if (this.userInfo.role !== 3) return false
      return this.order.reviewingAdminId && this.order.reviewingAdminId !== this.userInfo.id
    },
    canAccept() {
      if (!this.order || !this.userInfo) return false
      if (this.isRevoked) return false
      if (this.userInfo.role !== 2) return false
      if (this.order.auditStatus !== 1) return false
      if (this.order.status !== 0) return false
      if (this.order.acceptedCount >= this.order.peopleCount) return false
      if (this.myVolunteerRecord) return false
      return true
    },
    canVolunteerComplete() {
      if (!this.order || !this.userInfo) return false
      if (this.isRevoked) return false
      if (this.userInfo.role !== 2) return false
      if (this.order.status !== 1) return false
      if (!this.myVolunteerRecord) return false
      return this.myVolunteerRecord.status === 1
    },
    canVolunteerCancel() {
      if (!this.order || !this.userInfo) return false
      if (this.isRevoked) return false
      if (this.userInfo.role !== 2) return false
      if (!this.myVolunteerRecord) return false
      if (this.myVolunteerRecord.status !== 1) return false
      if (this.order.status !== 1) return false
      if (this.order.appointmentTime) {
        const deadline = new Date(this.order.appointmentTime.replace(/-/g, '/')).getTime() - 24 * 3600 * 1000
        if (Date.now() >= deadline) return false
      }
      return true
    },
    canConfirm() {
      if (!this.order || !this.userInfo) return false
      if (this.isRevoked) return false
      if (this.order.status !== 4) return false
      return this.userInfo.id === this.order.publisherId || this.userInfo.id === this.order.elderId
    },
    canAudit() {
      if (!this.order || !this.userInfo) return false
      if (this.isRevoked) return false
      if (this.userInfo.role !== 3 && this.userInfo.role !== 5) return false
      if (this.order.auditStatus !== 0 && this.order.auditStatus !== 3) return false
      if (this.userInfo.role === 3 && this.order.reviewingAdminId && this.order.reviewingAdminId !== this.userInfo.id) return false
      return true
    },
    canSuperDispatch() {
      if (!this.order || !this.userInfo) return false
      if (this.isRevoked) return false
      if (this.userInfo.role !== 5) return false
      return this.order.auditStatus === 0 || this.order.auditStatus === 3
    },
    canSuperRevoke() {
      if (!this.order || !this.userInfo) return false
      if (this.userInfo.role !== 5) return false
      // 已完成、已撤销不能撤销
      if (this.order.status === 2 || this.order.status === 5) return false
      return true
    },
    canEditOrCancelRejected() {
      if (!this.order || !this.userInfo) return false
      if (this.isRevoked) return false
      if (this.userInfo.role === 3 || this.userInfo.role === 5) return false
      if (this.order.auditStatus !== 2) return false
      return this.userInfo.id === this.order.publisherId || this.userInfo.id === this.order.elderId
    },
    canEditOrCancelNormal() {
      if (!this.order || !this.userInfo) return false
      if (this.isRevoked) return false
      if (this.userInfo.role === 3 || this.userInfo.role === 5) return false
      if (this.order.auditStatus !== 1 || this.order.status !== 0) return false
      return this.userInfo.id === this.order.publisherId || this.userInfo.id === this.order.elderId
    }
  },
  onLoad(options) {
    this.orderId = options.id
    this.userInfo = getUser() || {}
  },
  onShow() {
    this.load()
    if (this.userInfo.role === 3) {
      request({ url: `/order/lock/${this.orderId}?adminId=${this.userInfo.id}`, method: 'PUT' }).catch(() => {})
    }
  },
  onHide() {
    if (this.userInfo.role === 3 && this.order && this.order.auditStatus === 0 && this.order.status !== 5) {
      request({ url: `/order/unlock/${this.orderId}?adminId=${this.userInfo.id}`, method: 'PUT' }).catch(() => {})
    }
  },
  onUnload() {
    if (this.userInfo.role === 3 && this.order && this.order.auditStatus === 0 && this.order.status !== 5) {
      request({ url: `/order/unlock/${this.orderId}?adminId=${this.userInfo.id}`, method: 'PUT' }).catch(() => {})
    }
  },
  methods: {
    async load() {
      const res = await request({ url: `/order/detail/${this.orderId}` })
      this.order = res.order
      this.elderName = res.elderName
      this.volunteers = res.volunteers || []
      this.myVolunteerRecord = this.volunteers.find(v => v.volunteerId === this.userInfo.id) || null
    },
    roleText(role) {
      return { 1: '老人', 4: '子女', 5: '超级管理员' }[role] || '未知'
    },
    async acceptOrder() {
      try {
        await request({ url: `/order/accept/${this.orderId}?volunteerId=${this.userInfo.id}`, method: 'PUT' })
        uni.showToast({ title: '接单成功！' })
        this.load()
      } catch (e) { uni.showToast({ title: e.msg || '接单失败', icon: 'none' }) }
    },
    async completeMyPart() {
      await request({ url: `/order/complete/${this.orderId}?volunteerId=${this.userInfo.id}`, method: 'PUT' })
      uni.showToast({ title: '已提交，等待确认' })
      this.load()
    },
    volunteerCancel() {
      uni.showModal({
        title: '确认取消接单？', content: '取消后人数不足会重新出现在社区服务中',
        success: async (res) => {
          if (!res.confirm) return
          try {
            await request({ url: `/order/volunteer-cancel/${this.orderId}?volunteerId=${this.userInfo.id}`, method: 'DELETE' })
            uni.showToast({ title: '已取消接单' })
            this.load()
          } catch (e) { uni.showToast({ title: e.msg || '取消失败', icon: 'none' }) }
        }
      })
    },
    confirmOrder() {
      uni.showModal({
        title: '确认完成？', content: '确认后工单将标记为已完成',
        success: async (res) => {
          if (!res.confirm) return
          await request({ url: `/order/confirm/${this.orderId}`, method: 'PUT' })
          uni.showToast({ title: '已确认完成' })
          this.load()
        }
      })
    },
    async auditPass() {
      await request({ url: `/order/audit/${this.orderId}?auditStatus=1&adminId=${this.userInfo.id}`, method: 'PUT' })
      uni.showToast({ title: '已通过' })
      this.load()
    },
    async auditReject() {
      if (!this.rejectReason.trim()) return uni.showToast({ title: '请填写拒绝理由', icon: 'none' })
      await request({
        url: `/order/audit/${this.orderId}?auditStatus=2&rejectReason=${encodeURIComponent(this.rejectReason)}&adminId=${this.userInfo.id}`,
        method: 'PUT'
      })
      uni.showToast({ title: '已拒绝' })
      this.showRejectModal = false
      this.rejectReason = ''
      this.load()
    },
    async openDispatch() {
      try {
        this.adminList = await request({ url: `/user/adminList?adminId=${this.userInfo.id}` })
        this.showDispatchModal = true
      } catch (e) { uni.showToast({ title: '加载失败', icon: 'none' }) }
    },
    async dispatchTo(targetId) {
      await request({
        url: `/order/assign/${this.orderId}?adminId=${this.userInfo.id}&targetAdminId=${targetId}`,
        method: 'PUT'
      })
      uni.showToast({ title: '已分派' })
      this.showDispatchModal = false
      this.load()
    },
    superRevoke() {
      uni.showModal({
        title: '⚠️ 危险操作',
        content: '撤销后该工单永久报废，不可再编辑、接单、审核，也不可恢复',
        confirmColor: '#ff4d4f',
        success: async (res) => {
          if (!res.confirm) return
          try {
            await request({ url: `/order/revoke/${this.orderId}?adminId=${this.userInfo.id}`, method: 'PUT' })
            uni.showToast({ title: '已撤销' })
            this.load()
          } catch (e) { uni.showToast({ title: e.msg || '撤销失败', icon: 'none' }) }
        }
      })
    },
    openEditModal() {
      const o = this.order
      let date = '', time = ''
      if (o.appointmentTime) {
        date = o.appointmentTime.substring(0, 10)
        time = o.appointmentTime.substring(11, 16)
      }
      this.editForm = {
        serviceType: o.serviceType,
        address: o.address || '',
        contactPhone: o.contactPhone || '',
        peopleCount: o.peopleCount,
        appointmentDate: date,
        appointmentTimeOnly: time,
        remark: o.remark || ''
      }
      this.showEditModal = true
    },
    onEditType(e) { this.editForm.serviceType = Number(e.detail.value) + 1 },
    onEditDate(e) { this.editForm.appointmentDate = e.detail.value },
    onEditTime(e) { this.editForm.appointmentTimeOnly = e.detail.value },
    async saveEdit() {
      const f = this.editForm
      let appt = ''
      if (f.appointmentDate && f.appointmentTimeOnly) {
        appt = f.appointmentDate + ' ' + f.appointmentTimeOnly + ':00'
      }
      const payload = {
        serviceType: f.serviceType, address: f.address,
        contactPhone: f.contactPhone, peopleCount: Number(f.peopleCount) || 1,
        appointmentTime: appt, remark: f.remark
      }
      try {
        if (this.order.auditStatus === 2) {
          await request({ url: `/order/resubmit/${this.orderId}`, method: 'PUT', data: payload, header: { 'Content-Type': 'application/json' } })
          uni.showToast({ title: '已重新提交审核' })
        } else {
          await request({ url: `/order/modify/${this.orderId}`, method: 'PUT', data: payload, header: { 'Content-Type': 'application/json' } })
          uni.showToast({ title: '已修改，等待重新审核' })
        }
        this.showEditModal = false
        this.load()
      } catch (e) {
        uni.showToast({ title: e.msg || '操作失败', icon: 'none' })
      }
    },
    cancelOrder() {
      uni.showModal({
        title: '确认取消？', content: '取消后工单不再展示',
        success: async (res) => {
          if (!res.confirm) return
          try {
            await request({
              url: `/order/cancel/${this.orderId}?userId=${this.userInfo.id}&userRole=${this.userInfo.role}`,
              method: 'PUT'
            })
            uni.showToast({ title: '已取消' })
            this.load()
          } catch (e) { uni.showToast({ title: e.msg || '取消失败', icon: 'none' }) }
        }
      })
    }
  }
}
</script>
<style scoped>
.page-container { padding: 20rpx; }
.card { background: #fff; border-radius: 20rpx; padding: 28rpx; margin-bottom: 24rpx; }
.header { display: flex; justify-content: space-between; margin-bottom: 16rpx; }
.title { font-size: 34rpx; font-weight: 700; }
.tag { font-size: 24rpx; padding: 6rpx 18rpx; border-radius: 30rpx; color: #333; }
.info { font-size: 28rpx; color: #4a5a6e; margin-top: 12rpx; }
.section-title { font-size: 30rpx; font-weight: 600; margin-bottom: 16rpx; }
.vol-item { padding: 20rpx; border-bottom: 1rpx solid #f0f4f8; }
.vol-name { font-size: 30rpx; font-weight: 500; }
.vol-info { font-size: 24rpx; color: #8A9AAE; margin-top: 6rpx; }
.flag-warn { font-size: 22rpx; color: #faad14; background: #FFF3E0; padding: 2rpx 12rpx; border-radius: 20rpx; margin-left: 12rpx; }
.empty { text-align: center; color: #999; padding: 20rpx; font-size: 26rpx; }
.reject-box, .cancel-info, .reviewer-info { margin-top: 20rpx; padding: 20rpx; background: #FFE8E8; border-radius: 12rpx; }
.cancel-info { background: #FFF3E0; }
.reviewer-info { background: #E6FFED; }
.reject-label { font-size: 28rpx; font-weight: 600; color: #ff4d4f; margin-bottom: 8rpx; }
.cancel-info .reject-label { color: #d48806; }
.reviewer-info .reject-label { color: #52c41a; }
.reject-content { font-size: 28rpx; color: #4a5a6e; }
.revoke-banner {
  margin-top: 20rpx; padding: 24rpx; background: #FFF1F0;
  border: 2rpx solid #ff4d4f; border-radius: 12rpx;
  font-size: 30rpx; color: #ff4d4f; text-align: center; font-weight: 600;
}
.lock-tip { margin-top: 16rpx; padding: 16rpx; background: #E8F4FD; border-radius: 12rpx; font-size: 28rpx; color: #4A90D9; text-align: center; }
.btn-wrap { margin-top: 20rpx; }
.btn-accept { width: 100%; background: linear-gradient(135deg, #52c41a, #3da50e); color: #fff; border: none; border-radius: 50rpx; padding: 26rpx; font-size: 32rpx; font-weight: 600; }
.btn-confirm { width: 100%; background: linear-gradient(135deg, #4A90D9, #3b7bd9); color: #fff; border: none; border-radius: 50rpx; padding: 26rpx; font-size: 32rpx; font-weight: 600; }
.btn-cancel { width: 100%; background: #fff; color: #ff4d4f; border: 1px solid #ff4d4f; border-radius: 50rpx; padding: 22rpx; font-size: 30rpx; }
.btn-revoke { width: 100%; background: linear-gradient(135deg, #ff4d4f, #d9363e); color: #fff; border: none; border-radius: 50rpx; padding: 26rpx; font-size: 32rpx; font-weight: 600; box-shadow: 0 8rpx 24rpx rgba(255,77,79,0.35); }
.modal-mask { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; z-index: 999; }
.modal-box { width: 640rpx; max-height: 80vh; overflow-y: auto; background: #fff; border-radius: 24rpx; padding: 40rpx; box-sizing: border-box; }
.modal-title { font-size: 34rpx; font-weight: 700; text-align: center; margin-bottom: 30rpx; }
.form-row { display: flex; align-items: center; min-height: 90rpx; border-bottom: 1rpx solid #f0f4f8; padding: 10rpx 0; }
.form-row .label { width: 160rpx; flex-shrink: 0; font-size: 28rpx; color: #8A9AAE; }
.form-row input { flex: 1; height: 90rpx; line-height: 90rpx; font-size: 28rpx; background: transparent; border: none; }
.form-row picker { flex: 1; }
.picker-value { font-size: 28rpx; color: #1a2332; height: 90rpx; line-height: 90rpx; }
.textarea-row { align-items: flex-start; padding: 20rpx 0; }
.modal-textarea { flex: 1; width: 100%; min-height: 200rpx; font-size: 28rpx; background: #f7f9fc; border-radius: 12rpx; padding: 20rpx; box-sizing: border-box; border: 1px solid #e6edf4; }
.btn-primary { width: 100%; background: #4A90D9; color: #fff; border: none; border-radius: 50rpx; padding: 22rpx; font-size: 30rpx; margin-top: 20rpx; }
.dispatch-item { padding: 20rpx; border: 1rpx solid #eef2f7; border-radius: 12rpx; margin-bottom: 12rpx; }
.dispatch-name { font-size: 30rpx; font-weight: 500; }
.dispatch-desc { font-size: 24rpx; color: #8A9AAE; margin-top: 6rpx; }
</style>