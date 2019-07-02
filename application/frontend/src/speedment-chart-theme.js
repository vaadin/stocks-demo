import '@vaadin/vaadin-charts/vaadin-chart-default-theme.js'
import '@polymer/polymer/lib/elements/custom-style.js';
const documentContainer = document.createElement('template');

documentContainer.innerHTML = `
    <style include="vaadin-chart-default-theme">
      :host(.appreciating) .highcharts-color-0, :host(.appreciating) .highcharts-navigator-series {
        fill: #00dd00cc;
        stroke: #00dd00cc;
      }

      :host(.declining) .highcharts-color-0, :host(.declining) .highcharts-navigator-series {
        fill: #ff0000cc;
        stroke: #ff0000cc;
      }

      :host(.appreciating) .highcharts-navigator-mask-inside {
        fill: #00dd00cc;
      }

      :host(.declining) .highcharts-navigator-mask-inside {
        fill: #ff0000cc;
      }
    </style>
`;

document.head.appendChild(documentContainer.content);
