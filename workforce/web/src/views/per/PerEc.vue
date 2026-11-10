<template>
  <record-table kind="ec" :columns="columns" />
</template>

<script>
import RecordTable from "./RecordTable.vue";

export default {
  name: "PerEc",
  components: { RecordTable },
  data() {
    return {
      columns: [
        { prop: "ecDate", label: "奖惩日期", type: "date", width: 120 },
        { prop: "ecReason", label: "奖惩原因", minWidth: 220, textarea: true },
        { prop: "ecPoint", label: "奖惩分数", type: "number", width: 110 },
        { prop: "ecType", label: "类型(1奖2惩)", type: "number", width: 120 },
        { prop: "remark", label: "备注", minWidth: 160, textarea: true }
      ]
    };
  }
};
</script>
