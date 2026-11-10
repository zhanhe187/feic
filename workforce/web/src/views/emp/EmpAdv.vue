<template>
  <div class="page">
    <el-card shadow="never">
      <div class="toolbar">
        <el-input v-model="query.name" placeholder="按姓名搜索" clearable style="width: 220px"
                  @keyup.enter.native="load" @clear="load" />
        <el-button type="primary" icon="el-icon-search" @click="load">查询</el-button>
        <span class="hint">高级资料汇总：学历、政治面貌、合同与转正信息</span>
      </div>

      <el-table :data="rows" v-loading="loading" border size="mini">
        <el-table-column prop="name" label="姓名" width="110" />
        <el-table-column prop="workID" label="工号" width="110" />
        <el-table-column prop="tiptopDegree" label="最高学历" width="110" />
        <el-table-column prop="specialty" label="专业" min-width="150" show-overflow-tooltip />
        <el-table-column prop="school" label="毕业院校" min-width="160" show-overflow-tooltip />
        <el-table-column prop="engageForm" label="聘用形式" width="110" />
        <el-table-column prop="contractTerm" label="合同年限" width="100" />
        <el-table-column label="合同到期" width="120">
          <template slot-scope="scope">{{ date(scope.row.endContract) }}</template>
        </el-table-column>
        <el-table-column label="转正日期" width="120">
          <template slot-scope="scope">{{ date(scope.row.conversionTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="90" fixed="right">
          <template slot-scope="scope">
            <el-button type="text" @click="detail(scope.row)">查看档案</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination class="pager" background layout="total, prev, pager, next"
                     :total="total" :current-page.sync="query.page" :page-size="query.size"
                     @current-change="load" />
    </el-card>

    <el-dialog title="高级资料" :visible.sync="dialogVisible" width="680px">
      <el-descriptions :column="2" border size="small">
        <el-descriptions-item label="姓名">{{ current.name }}</el-descriptions-item>
        <el-descriptions-item label="性别">{{ current.gender }}</el-descriptions-item>
        <el-descriptions-item label="出生日期">{{ date(current.birthday) }}</el-descriptions-item>
        <el-descriptions-item label="身份证号">{{ current.idCard }}</el-descriptions-item>
        <el-descriptions-item label="民族">{{ current.nationName }}</el-descriptions-item>
        <el-descriptions-item label="政治面貌">{{ current.politicName }}</el-descriptions-item>
        <el-descriptions-item label="籍贯">{{ current.nativePlace }}</el-descriptions-item>
        <el-descriptions-item label="婚姻状况">{{ current.wedlock }}</el-descriptions-item>
        <el-descriptions-item label="最高学历">{{ current.tiptopDegree }}</el-descriptions-item>
        <el-descriptions-item label="专业">{{ current.specialty }}</el-descriptions-item>
        <el-descriptions-item label="毕业院校">{{ current.school }}</el-descriptions-item>
        <el-descriptions-item label="聘用形式">{{ current.engageForm }}</el-descriptions-item>
        <el-descriptions-item label="合同起始">{{ date(current.beginContract) }}</el-descriptions-item>
        <el-descriptions-item label="合同到期">{{ date(current.endContract) }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script>
export default {
  name: "EmpAdv",
  data() {
    return {
      loading: false,
      rows: [],
      total: 0,
      query: { page: 1, size: 10, name: "" },
      dialogVisible: false,
      current: {}
    };
  },
  mounted() {
    this.load();
  },
  methods: {
    load() {
      this.loading = true;
      this.getRequest("/employee/basic/?page=" + this.query.page + "&size=" + this.query.size +
        "&name=" + encodeURIComponent(this.query.name || ""))
        .then((resp) => {
          this.loading = false;
          if (resp) {
            this.rows = resp.data || [];
            this.total = resp.total || 0;
          }
        })
        .catch(() => { this.loading = false; });
    },
    detail(row) {
      this.current = row;
      this.dialogVisible = true;
    },
    date(value) {
      return value ? String(value).substring(0, 10) : "-";
    }
  }
};
</script>

<style scoped>
.page { padding: 8px; }
.toolbar { display: flex; align-items: center; gap: 10px; margin-bottom: 12px; }
.hint { color: #909399; font-size: 12px; }
.pager { margin-top: 12px; text-align: right; }
</style>
