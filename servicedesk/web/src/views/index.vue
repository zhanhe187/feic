<template>
  <div class="dashboard app-container">
    <section class="overview">
      <div>
        <p class="eyebrow">IT Operations Console</p>
        <h2>IT 工单与资产管理平台</h2>
        <p class="summary">
          面向企业内部 IT 运维场景，覆盖资产台账、服务工单、处理记录、超时提醒和权限控制，
          帮助员工提交问题、IT 人员闭环处理、管理员维护资产数据。
        </p>
      </div>
      <div class="version">
        <span>当前版本</span>
        <strong>v3.9.2</strong>
      </div>
    </section>

    <el-row :gutter="16" class="metric-row">
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card shadow="never">
          <div class="metric">
            <span>资产台账</span>
            <strong>统一维护</strong>
            <p>电脑、显示器、外设等设备的编号、状态和使用人。</p>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card shadow="never">
          <div class="metric">
            <span>工单流转</span>
            <strong>闭环处理</strong>
            <p>创建、分配、完成、驳回、关闭均写入处理记录。</p>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card shadow="never">
          <div class="metric">
            <span>权限控制</span>
            <strong>角色隔离</strong>
            <p>员工、IT 处理人和管理员按菜单权限访问数据。</p>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card shadow="never">
          <div class="metric">
            <span>定时提醒</span>
            <strong>超时扫描</strong>
            <p>Quartz 定时扫描超时工单，并写入系统通知。</p>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :xs="24" :lg="14">
        <el-card shadow="never" class="panel-card">
          <template #header>
            <span>核心流程</span>
          </template>
          <el-steps :active="4" finish-status="success" align-center>
            <el-step title="提交工单" description="员工描述问题和优先级" />
            <el-step title="分配处理" description="IT 人员接单并记录处理人" />
            <el-step title="完成/驳回" description="按状态机执行合法流转" />
            <el-step title="关闭归档" description="保留完整处理痕迹" />
          </el-steps>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="10">
        <el-card shadow="never" class="panel-card">
          <template #header>
            <span>技术栈</span>
          </template>
          <div class="stack-list">
            <div>
              <span>后端</span>
              <p>Spring Boot 4, Spring Security, MyBatis, Druid, Quartz, Redis</p>
            </div>
            <div>
              <span>前端</span>
              <p>Vue 3, Vite, Element Plus, Pinia, Axios</p>
            </div>
            <div>
              <span>数据库</span>
              <p>MySQL 8, 资产表、工单表、处理记录表</p>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="panel-card">
      <template #header>
        <span>模块入口</span>
      </template>
      <div class="module-links">
        <el-button type="primary" plain icon="Tickets" @click="goTarget('/it/ticket')">工单管理</el-button>
        <el-button type="success" plain icon="Box" @click="goTarget('/it/asset')">资产台账</el-button>
        <el-button type="warning" plain icon="Bell" @click="goTarget('/system/notice')">系统通知</el-button>
        <el-button type="info" plain icon="Operation" @click="goTarget('/monitor/job')">定时任务</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup name="Index">
const router = useRouter()

function goTarget(path) {
  router.push(path)
}
</script>

<style scoped lang="scss">
.overview {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  padding: 24px;
  margin-bottom: 16px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  background: var(--el-bg-color);
}

.eyebrow {
  margin: 0 0 8px;
  color: var(--el-color-primary);
  font-size: 13px;
  font-weight: 700;
}

.overview h2 {
  margin: 0 0 12px;
  font-size: 24px;
  font-weight: 600;
}

.summary {
  max-width: 760px;
  margin: 0;
  line-height: 1.8;
  color: var(--el-text-color-regular);
}

.version {
  min-width: 132px;
  align-self: flex-start;
  padding: 14px 16px;
  border-radius: 8px;
  background: var(--el-fill-color-light);
}

.version span,
.metric span,
.stack-list span {
  display: block;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.version strong {
  display: block;
  margin-top: 4px;
  font-size: 22px;
}

.metric-row {
  margin-bottom: 16px;
}

.metric {
  min-height: 118px;
}

.metric strong {
  display: block;
  margin: 8px 0;
  font-size: 20px;
}

.metric p,
.stack-list p {
  margin: 0;
  line-height: 1.7;
  color: var(--el-text-color-regular);
}

.panel-card {
  margin-bottom: 16px;
  border-radius: 8px;
}

.stack-list {
  display: grid;
  gap: 16px;
}

.module-links {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

@media (max-width: 768px) {
  .overview {
    flex-direction: column;
    padding: 18px;
  }
}
</style>
