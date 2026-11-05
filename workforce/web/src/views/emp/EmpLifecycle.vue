<template>
    <div class="lifecycle-page">
        <div class="toolbar">
            <div class="filters">
                <el-input
                        v-model="query.name"
                        clearable
                        prefix-icon="el-icon-search"
                        placeholder="员工姓名"
                        class="filter-item"
                        @clear="loadCurrent"
                        @keydown.enter.native="loadCurrent">
                </el-input>
                <el-input
                        v-model="query.departmentId"
                        clearable
                        placeholder="部门ID"
                        class="filter-item small"
                        @clear="loadCurrent"
                        @keydown.enter.native="loadCurrent">
                </el-input>
                <el-button type="primary" icon="el-icon-search" @click="loadCurrent">查询</el-button>
                <el-button icon="el-icon-refresh" @click="resetQuery">重置</el-button>
            </div>
            <el-button type="success" icon="el-icon-bell" @click="scanLifecycle">扫描提醒</el-button>
        </div>

        <el-tabs v-model="activeTab" @tab-click="handleTabChange">
            <el-tab-pane label="待转正员工" name="probation">
                <el-table :data="probation.data" stripe border v-loading="loading" class="data-table">
                    <el-table-column prop="id" label="ID" width="70"></el-table-column>
                    <el-table-column prop="name" label="姓名" width="110"></el-table-column>
                    <el-table-column label="部门" min-width="130">
                        <template slot-scope="scope">
                            {{ scope.row.department ? scope.row.department.name : '-' }}
                        </template>
                    </el-table-column>
                    <el-table-column prop="workState" label="员工状态" width="110"></el-table-column>
                    <el-table-column prop="beginDate" label="入职日期" width="130"></el-table-column>
                    <el-table-column prop="conversionTime" label="转正日期" width="130"></el-table-column>
                    <el-table-column prop="lifecycleReminderStatus" label="提醒状态" width="120">
                        <template slot-scope="scope">
                            <el-tag size="mini" :type="tagType(scope.row.lifecycleReminderStatus)">
                                {{ scope.row.lifecycleReminderStatus || 'PENDING' }}
                            </el-tag>
                        </template>
                    </el-table-column>
                    <el-table-column label="操作" width="190" fixed="right">
                        <template slot-scope="scope">
                            <el-button type="primary" size="mini" @click="regularize(scope.row)">转正</el-button>
                            <el-button size="mini" @click="openActions(scope.row)">跟进</el-button>
                        </template>
                    </el-table-column>
                </el-table>
                <div class="pager">
                    <el-pagination
                            background
                            layout="prev, pager, next, sizes, total"
                            :page-size="probation.size"
                            :current-page="probation.page"
                            :total="probation.total"
                            @current-change="page => handlePageChange('probation', page)"
                            @size-change="size => handleSizeChange('probation', size)">
                    </el-pagination>
                </div>
            </el-tab-pane>

            <el-tab-pane label="合同即将到期" name="contract">
                <el-table :data="contract.data" stripe border v-loading="loading" class="data-table">
                    <el-table-column prop="id" label="ID" width="70"></el-table-column>
                    <el-table-column prop="name" label="姓名" width="110"></el-table-column>
                    <el-table-column label="部门" min-width="130">
                        <template slot-scope="scope">
                            {{ scope.row.department ? scope.row.department.name : '-' }}
                        </template>
                    </el-table-column>
                    <el-table-column prop="workState" label="员工状态" width="110"></el-table-column>
                    <el-table-column prop="beginContract" label="合同开始" width="130"></el-table-column>
                    <el-table-column prop="endContract" label="合同到期" width="130"></el-table-column>
                    <el-table-column prop="contractTerm" label="合同年限" width="100"></el-table-column>
                    <el-table-column prop="lifecycleReminderStatus" label="提醒状态" width="110">
                        <template slot-scope="scope">
                            <el-tag size="mini" :type="tagType(scope.row.lifecycleReminderStatus)">
                                {{ scope.row.lifecycleReminderStatus || 'PENDING' }}
                            </el-tag>
                        </template>
                    </el-table-column>
                    <el-table-column label="操作" width="260" fixed="right">
                        <template slot-scope="scope">
                            <el-button type="primary" size="mini" @click="openRenew(scope.row)">续签</el-button>
                            <el-button type="warning" size="mini" @click="openNotRenew(scope.row)">不续签</el-button>
                            <el-button size="mini" @click="openActions(scope.row)">跟进</el-button>
                        </template>
                    </el-table-column>
                </el-table>
                <div class="pager">
                    <el-pagination
                            background
                            layout="prev, pager, next, sizes, total"
                            :page-size="contract.size"
                            :current-page="contract.page"
                            :total="contract.total"
                            @current-change="page => handlePageChange('contract', page)"
                            @size-change="size => handleSizeChange('contract', size)">
                    </el-pagination>
                </div>
            </el-tab-pane>
        </el-tabs>

        <!-- 续签合同 -->
        <el-dialog title="续签合同" :visible.sync="renewOpen" width="480px" append-to-body>
            <el-form label-width="100px">
                <el-form-item label="员工">
                    <span>{{ renewRow ? renewRow.name : '' }}（当前到期：{{ renewRow ? renewRow.endContract : '-' }}）</span>
                </el-form-item>
                <el-form-item label="新合同期限">
                    <el-date-picker
                            v-model="renewForm.range"
                            type="daterange"
                            value-format="yyyy-MM-dd"
                            range-separator="至"
                            start-placeholder="起始日"
                            end-placeholder="到期日"
                            style="width: 100%">
                    </el-date-picker>
                </el-form-item>
                <el-form-item label="续签说明">
                    <el-input v-model="renewForm.reason" type="textarea" :rows="2" placeholder="选填，例如：绩效达标，续签一年"></el-input>
                </el-form-item>
            </el-form>
            <span slot="footer">
                <el-button type="primary" :loading="submitting" @click="submitRenew">确定</el-button>
                <el-button @click="renewOpen = false">取消</el-button>
            </span>
        </el-dialog>

        <!-- 不续签 -->
        <el-dialog title="标记不续签" :visible.sync="notRenewOpen" width="480px" append-to-body>
            <el-form label-width="100px">
                <el-form-item label="员工">
                    <span>{{ notRenewRow ? notRenewRow.name : '' }}（合同到期：{{ notRenewRow ? notRenewRow.endContract : '-' }}）</span>
                </el-form-item>
                <el-form-item label="不续签原因">
                    <el-input v-model="notRenewReason" type="textarea" :rows="3" placeholder="必填，例如：岗位取消 / 绩效未达标"></el-input>
                </el-form-item>
            </el-form>
            <el-alert type="info" :closable="false" show-icon
                      title="只记录决策与原因，不会自动把员工改成离职（离职属于人事处理）"></el-alert>
            <span slot="footer">
                <el-button type="primary" :loading="submitting" @click="submitNotRenew">确定</el-button>
                <el-button @click="notRenewOpen = false">取消</el-button>
            </span>
        </el-dialog>

        <!-- 跟进记录 -->
        <el-dialog title="跟进记录" :visible.sync="actionsOpen" width="620px" append-to-body>
            <div style="margin-bottom: 8px; color: #909399">
                {{ actionsRow ? actionsRow.name : '' }} · 共 {{ actions.length }} 条
            </div>
            <el-table :data="actions" size="small" border>
                <el-table-column label="动作" width="90">
                    <template slot-scope="scope">{{ actionLabel(scope.row.actionType) }}</template>
                </el-table-column>
                <el-table-column label="到期日变化" width="200">
                    <template slot-scope="scope">
                        {{ formatDate(scope.row.beforeEndContract) }} → {{ formatDate(scope.row.afterEndContract) }}
                    </template>
                </el-table-column>
                <el-table-column prop="reason" label="说明" min-width="200" show-overflow-tooltip></el-table-column>
                <el-table-column prop="operator" label="操作人" width="90"></el-table-column>
                <el-table-column label="时间" width="160"><template slot-scope="scope">{{ formatDateTime(scope.row.createTime) }}</template></el-table-column>
            </el-table>
            <el-empty v-if="actions.length === 0" description="暂无跟进记录"></el-empty>
        </el-dialog>
    </div>
</template>

<script>
export default {
    name: "EmpLifecycle",
    data() {
        return {
            loading: false,
            submitting: false,
            activeTab: "probation",
            query: {
                name: "",
                departmentId: ""
            },
            probation: {
                page: 1,
                size: 10,
                total: 0,
                data: []
            },
            contract: {
                page: 1,
                size: 10,
                total: 0,
                data: []
            },
            renewOpen: false,
            renewRow: null,
            renewForm: {range: [], reason: ""},
            notRenewOpen: false,
            notRenewRow: null,
            notRenewReason: "",
            actionsOpen: false,
            actionsRow: null,
            actions: []
        }
    },
    mounted() {
        this.loadCurrent();
    },
    methods: {
        handleTabChange() {
            this.loadCurrent();
        },
        loadCurrent() {
            if (this.activeTab === "probation") {
                this.loadProbation();
            } else {
                this.loadContract();
            }
        },
        buildParams(state) {
            const params = {
                page: state.page,
                size: state.size
            };
            if (this.query.name) {
                params.name = this.query.name;
            }
            if (this.query.departmentId) {
                params.departmentId = this.query.departmentId;
            }
            return params;
        },
        loadProbation() {
            this.loading = true;
            this.getRequest("/employee/basic/probation/remind", this.buildParams(this.probation)).then(resp => {
                this.loading = false;
                if (resp) {
                    this.probation.data = resp.data || [];
                    this.probation.total = resp.total || 0;
                }
            });
        },
        loadContract() {
            this.loading = true;
            this.getRequest("/employee/basic/contract/remind", this.buildParams(this.contract)).then(resp => {
                this.loading = false;
                if (resp) {
                    this.contract.data = resp.data || [];
                    this.contract.total = resp.total || 0;
                }
            });
        },
        resetQuery() {
            this.query.name = "";
            this.query.departmentId = "";
            this.probation.page = 1;
            this.contract.page = 1;
            this.loadCurrent();
        },
        handlePageChange(type, page) {
            this[type].page = page;
            this.loadCurrent();
        },
        handleSizeChange(type, size) {
            this[type].size = size;
            this[type].page = 1;
            this.loadCurrent();
        },
        regularize(row) {
            this.$confirm("确认将该员工转为正式员工？", "转正确认", {type: "warning"}).then(() => {
                this.putRequest("/employee/basic/" + row.id + "/regularize").then(resp => {
                    if (resp) {
                        this.loadProbation();
                    }
                });
            }).catch(() => {
            });
        },
        scanLifecycle() {
            this.postRequest("/employee/basic/lifecycle/scan").then(resp => {
                if (resp) {
                    this.loadCurrent();
                }
            });
        },
        openRenew(row) {
            this.renewRow = row;
            this.renewForm = {range: [], reason: ""};
            this.renewOpen = true;
        },
        submitRenew() {
            if (!this.renewForm.range || this.renewForm.range.length !== 2) {
                this.$message.warning("请选择新合同起止日期");
                return;
            }
            this.submitting = true;
            this.putRequest("/employee/basic/" + this.renewRow.id + "/renew", {
                beginDate: this.renewForm.range[0],
                endDate: this.renewForm.range[1],
                reason: this.renewForm.reason
            }).then(resp => {
                this.submitting = false;
                if (resp) {
                    this.renewOpen = false;
                    this.loadContract();
                }
            }).catch(() => {
                this.submitting = false;
            });
        },
        openNotRenew(row) {
            this.notRenewRow = row;
            this.notRenewReason = "";
            this.notRenewOpen = true;
        },
        submitNotRenew() {
            if (!this.notRenewReason || !this.notRenewReason.trim()) {
                this.$message.warning("请填写不续签原因");
                return;
            }
            this.submitting = true;
            this.putRequest("/employee/basic/" + this.notRenewRow.id + "/not-renew", {
                reason: this.notRenewReason
            }).then(resp => {
                this.submitting = false;
                if (resp) {
                    this.notRenewOpen = false;
                    this.loadContract();
                }
            }).catch(() => {
                this.submitting = false;
            });
        },
        openActions(row) {
            this.actionsRow = row;
            this.actions = [];
            this.actionsOpen = true;
            this.getRequest("/employee/basic/" + row.id + "/lifecycle/actions").then(resp => {
                if (resp) {
                    this.actions = resp || [];
                }
            });
        },
        formatDate(value) {
            if (!value) {
                return '-';
            }
            const d = new Date(value);
            if (isNaN(d.getTime())) {
                return value;
            }
            const pad = n => (n < 10 ? '0' + n : '' + n);
            return d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate());
        },
        formatDateTime(value) {
            if (!value) {
                return '-';
            }
            const d = new Date(value);
            if (isNaN(d.getTime())) {
                return value;
            }
            const pad = n => (n < 10 ? '0' + n : '' + n);
            return this.formatDate(value) + ' ' + pad(d.getHours()) + ':' + pad(d.getMinutes());
        },
        actionLabel(type) {
            const map = {
                REGULARIZE: "转正",
                RENEW: "续签",
                NOT_RENEW: "不续签"
            };
            return map[type] || type;
        },
        tagType(status) {
            if (status === "DONE") {
                return "success";
            }
            if (status === "SENT") {
                return "warning";
            }
            return "info";
        }
    }
}
</script>

<style scoped>
.lifecycle-page {
    padding: 10px;
}

.toolbar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 12px;
}

.filters {
    display: flex;
    align-items: center;
}

.filter-item {
    width: 220px;
    margin-right: 8px;
}

.filter-item.small {
    width: 120px;
}

.data-table {
    width: 100%;
}

.pager {
    display: flex;
    justify-content: flex-end;
    margin-top: 12px;
}
</style>
