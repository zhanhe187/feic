<template>
  <record-table kind="move" :columns="columns" />
</template>

<script>
import RecordTable from "./RecordTable.vue";

export default {
  name: "PerMv",
  components: { RecordTable },
  data() {
    return {
      columns: [
        { prop: "removeDate", label: "调动日期", type: "date", width: 120 },
        { prop: "afterDepId", label: "新部门ID", type: "number", width: 120 },
        { prop: "afterJobId", label: "新职称ID", type: "number", width: 120 },
        { prop: "reason", label: "调动原因", minWidth: 220, textarea: true },
        { prop: "remark", label: "备注", minWidth: 160, textarea: true }
      ]
    };
  }
};
</script>
