<template>
  <div class="record-page">
    <el-card shadow="never">
      <div class="toolbar">
        <el-input v-model="query.name" placeholder="按员工姓名搜索" clearable style="width: 220px"
                  @keyup.enter.native="load" @clear="load" />
        <el-button type="primary" icon="el-icon-search" @click="load">查询</el-button>
        <el-button type="success" icon="el-icon-plus" @click="openAdd">新增</el-button>
        <span class="total">共 {{ total }} 条</span>
      </div>

      <el-table :data="rows" v-loading="loading" border size="mini" style="width: 100%">
        <el-table-column prop="empName" label="员工" width="120" />
        <el-table-column prop="depName" label="部门" width="140" />
        <el-table-column v-for="col in columns" :key="col.prop" :prop="col.prop" :label="col.label"
                         :width="col.width" :min-width="col.minWidth" show-overflow-tooltip>
          <template slot-scope="scope">
            <span>{{ format(col, scope.row[col.prop]) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template slot-scope="scope">
            <el-button type="text" @click="openEdit(scope.row)">编辑</el-button>
            <el-button type="text" style="color:#f56c6c" @click="remove(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination class="pager" background layout="total, prev, pager, next, sizes"
                     :total="total" :current-page.sync="query.page" :page-size.sync="query.size"
                     :page-sizes="[10, 20, 50]" @current-change="load" @size-change="load" />

      <el-dialog :title="form.id ? '编辑记录' : '新增记录'" :visible.sync="dialogVisible" width="520px">
        <el-form :model="form" label-width="100px" size="small">
          <el-form-item label="员工">
            <el-select v-model="form.eid" filterable remote clearable placeholder="输入姓名搜索"
                       :remote-method="searchEmp" :loading="empLoading" style="width: 100%">
              <el-option v-for="e in empOptions" :key="e.id" :label="e.name + '（' + (e.workID || e.id) + '）'" :value="e.id" />
            </el-select>
          </el-form-item>
          <el-form-item v-for="col in columns" :key="col.prop" :label="col.label">
            <el-date-picker v-if="col.type === 'date'" v-model="form[col.prop]" type="date"
                            value-format="yyyy-MM-dd" style="width: 100%" />
            <el-input-number v-else-if="col.type === 'number'" v-model="form[col.prop]" :min="0" style="width: 100%" />
            <el-input v-else v-model="form[col.prop]" :type="col.textarea ? 'textarea' : 'text'" :rows="2" />
          </el-form-item>
        </el-form>
        <span slot="footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="save">确定</el-button>
        </span>
      </el-dialog>
    </el-card>
  </div>
</template>

<script>
export default {
  name: "RecordTable",
  props: {
    kind: { type: String, required: true },
    columns: { type: Array, required: true }
  },
  data() {
    return {
      loading: false,
      rows: [],
      total: 0,
      query: { page: 1, size: 10, name: "" },
      dialogVisible: false,
      form: {},
      empOptions: [],
      empLoading: false
    };
  },
  watch: {
    kind() {
      this.load();
    }
  },
  mounted() {
    this.load();
  },
  methods: {
    load() {
      this.loading = true;
      this.getRequest("/personnel/" + this.kind + "?page=" + this.query.page + "&size=" + this.query.size +
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
    searchEmp(keyword) {
      this.empLoading = true;
      this.getRequest("/employee/basic/?page=1&size=20&name=" + encodeURIComponent(keyword || ""))
        .then((resp) => {
          this.empLoading = false;
          this.empOptions = (resp && resp.data) || [];
        })
        .catch(() => { this.empLoading = false; });
    },
    openAdd() {
      this.form = {};
      this.dialogVisible = true;
      if (this.empOptions.length === 0) {
        this.searchEmp("");
      }
    },
    openEdit(row) {
      this.form = Object.assign({}, row);
      this.dialogVisible = true;
      if (this.empOptions.length === 0) {
        this.searchEmp(row.empName || "");
      }
    },
    save() {
      if (!this.form.eid) {
        this.$message.error("请先选择员工");
        return;
      }
      const payload = {};
      this.columns.forEach((col) => {
        payload[col.prop] = this.form[col.prop];
      });
      payload.eid = this.form.eid;
      const isEdit = !!this.form.id;
      const req = isEdit
        ? this.putRequest("/personnel/" + this.kind + "/" + this.form.id, payload)
        : this.postRequest("/personnel/" + this.kind, payload);
      req.then((resp) => {
        if (resp && resp.status === 200) {
          this.$message.success(resp.msg || "保存成功");
          this.dialogVisible = false;
          this.load();
        } else {
          this.$message.error((resp && resp.msg) || "保存失败");
        }
      });
    },
    remove(row) {
      this.$confirm("确定删除这条记录吗？", "提示", { type: "warning" }).then(() => {
        this.deleteRequest("/personnel/" + this.kind + "/" + row.id).then((resp) => {
          if (resp && resp.status === 200) {
            this.$message.success("删除成功");
            this.load();
          } else {
            this.$message.error((resp && resp.msg) || "删除失败");
          }
        });
      }).catch(() => {});
    },
    format(col, value) {
      if (value === null || value === undefined || value === "") {
        return "-";
      }
      if (col.type === "date") {
        return String(value).substring(0, 10);
      }
      return value;
    }
  }
};
</script>

<style scoped>
.record-page { padding: 8px; }
.toolbar { display: flex; align-items: center; gap: 10px; margin-bottom: 12px; }
.total { margin-left: auto; color: #909399; font-size: 12px; }
.pager { margin-top: 12px; text-align: right; }
</style>
