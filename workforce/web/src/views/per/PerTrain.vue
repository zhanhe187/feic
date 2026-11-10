<template>
  <record-table kind="train" :columns="columns" />
</template>

<script>
import RecordTable from "./RecordTable.vue";

export default {
  name: "PerTrain",
  components: { RecordTable },
  data() {
    return {
      columns: [
        { prop: "trainDate", label: "培训日期", type: "date", width: 120 },
        { prop: "trainContent", label: "培训内容", minWidth: 260, textarea: true },
        { prop: "remark", label: "备注", minWidth: 180, textarea: true }
      ]
    };
  }
};
</script>
