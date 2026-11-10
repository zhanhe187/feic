<template>
  <record-table kind="adjust" :columns="columns" />
</template>

<script>
import RecordTable from "./RecordTable.vue";

export default {
  name: "PerSalary",
  components: { RecordTable },
  data() {
    return {
      columns: [
        { prop: "asDate", label: "调薪日期", type: "date", width: 120 },
        { prop: "beforeSalary", label: "调整前", type: "number", width: 120 },
        { prop: "afterSalary", label: "调整后", type: "number", width: 120 },
        { prop: "reason", label: "调薪原因", minWidth: 200, textarea: true },
        { prop: "remark", label: "备注", minWidth: 160, textarea: true }
      ]
    };
  }
};
</script>
