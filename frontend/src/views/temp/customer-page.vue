
<article :class="cardClass">
    <div class="card-head">
      <div class="card-title">
        <slot name="title" />
      </div>
    </div>
    <slot />
  </article>


<article v-if="productBehaviorChartData"
  class="card r20 pad12 grow-chart product-behavior-card"
  @click="toggleProductBehaviorPopover">

  <div class="chart-box h190 product-behavior-split">
    <bar
      v-if="productBehaviorLeftChartData"
      aria-label="產品行為指標（存貨比）"
      :labels="productBehaviorLeftChartData.labels"
      :customer-data="productBehaviorLeftChartData.customerData"
      :cluster-data="productBehaviorLeftChartData.clusterData"
      :y-max="productBehaviorLeftChartData.yMax"
      :step-size="productBehaviorLeftChartData.stepSize"
      customer-label="客群Top10%"
      cluster-label="客群"
      y-tick-suffix="%"
      wrapper-class="mini-chart"
      :show-legend="false"
      :bar-thickness="16"
    />
    <bar
      v-if="productBehaviorRightChartData"
      :show-y-axis="false"
      aria-label="產品行為指標［活存比與理財AUM比］"
      :labels="productBehaviorRightChartData.labels"
      :customer-data="productBehaviorRightChartData.customerData"
      :cluster-data="productBehaviorRightChartData.clusterData"
      :y-max="productBehaviorRightChartData.yMax"
      :step-size="productBehaviorRightChartData.stepSize"
      customer-label="客群Top10%"
      cluster-label="客群"
      y-tick-suffix="%"
      wrapper-class="mini-chart"
      :show-legend="false"
      :bar-thickness="16"
    />
  </div>
</article><article v-if="productBehaviorChartData"
  class="card r20 pad12 grow-chart product-behavior-card"
  @click="toggleProductBehaviorPopover">

  <div class="chart-box h190 product-behavior-split">
    <bar
      v-if="productBehaviorLeftChartData"
      aria-label="產品行為指標（存貨比）"
      :labels="productBehaviorLeftChartData.labels"
      :customer-data="productBehaviorLeftChartData.customerData"
      :cluster-data="productBehaviorLeftChartData.clusterData"
      :y-max="productBehaviorLeftChartData.yMax"
      :step-size="productBehaviorLeftChartData.stepSize"
      customer-label="客群Top10%"
      cluster-label="客群"
      y-tick-suffix="%"
      wrapper-class="mini-chart"
      :show-legend="false"
      :bar-thickness="16"
    />
    <bar
      v-if="productBehaviorRightChartData"
      :show-y-axis="false"
      aria-label="產品行為指標［活存比與理財AUM比］"
      :labels="productBehaviorRightChartData.labels"
      :customer-data="productBehaviorRightChartData.customerData"
      :cluster-data="productBehaviorRightChartData.clusterData"
      :y-max="productBehaviorRightChartData.yMax"
      :step-size="productBehaviorRightChartData.stepSize"
      customer-label="客群Top10%"
      cluster-label="客群"
      y-tick-suffix="%"
      wrapper-class="mini-chart"
      :show-legend="false"
      :bar-thickness="16"
    />
  </div>
</article>