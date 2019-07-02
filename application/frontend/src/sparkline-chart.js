import {PolymerElement, html} from '@polymer/polymer';
import {ChartElement} from '@vaadin/vaadin-charts/src/vaadin-chart.js';
import './speedment-chart-theme';

class SparklineChart extends ChartElement {

  static get template() {
    return html`
      <style include="speedment-chart-theme">
      .highcharts-axis, .highcharts-grid, .highcharts-markers, .highcharts-tracker, .highcharts-background {
        display: none;
      }

      .highcharts-graph {
        stroke-width: 1px;
      }

      .highcharts-graph:hover {
        animation: breathe 5s ease-out infinite;
      }

      @keyframes breathe {
        0% { stroke-width: 1px }
        40% { stroke-width: 3px }
        60% { stroke-width: 3px }
        100% { stroke-width: 1px }
      }
    </style>

    <div id="chart"></div>
    <slot id="slot"></slot>
    `;
  }

  static get is() {
    return 'sparkline-chart'
  }

  connectedCallback() {
    this.update({
      chart: {
        animation: false,
        backgroundColor: null,
        borderWidth: 0,
        type: 'line',
        margin: [2, 0, 2, 0],
        width: 120,
        height: 20,
        style: {
          overflow: 'visible'
        },
        skipClone: true
      },
      title: {
        text: ''
      },
      credits: {
        enabled: false
      },
      xAxis: {
        labels: {
          enabled: false
        },
        title: {
          text: null
        },
        startOnTick: false,
        endOnTick: false,
        tickPositions: []
      },
      yAxis: {
        endOnTick: false,
        startOnTick: false,
        labels: {
          enabled: false
        },
        title: {
          text: null
        },
        tickPositions: [0]
      },
      legend: {
        enabled: false
      },
      exporting: {
        enabled: false
      },
      tooltip: {
        enabled: false
      },
      plotOptions: {
        series: {
          animation: false,
          lineWidth: 1,
          shadow: false,
          fillOpacity: 0.25,
          marker: {
            enabled: false
          }
        }
      }
    });
    super.connectedCallback();
  }
}

customElements.define(SparklineChart.is, SparklineChart);
