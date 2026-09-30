import { AfterViewInit, Component, ElementRef, Input, OnChanges, OnDestroy, SimpleChanges, ViewChild } from '@angular/core';
import { Chart as ChartJs, ChartData, ChartOptions, ChartType } from 'chart.js/auto';

@Component({
  selector: 'app-chart',
  standalone: true,
  template: `<canvas #canvas role="img"></canvas>`
})
export class ChartComponent implements AfterViewInit, OnChanges, OnDestroy {
  @Input() type: ChartType = 'bar';
  @Input() data: ChartData = { labels: [], datasets: [] };
  @Input() options: ChartOptions = { responsive: true };
  @ViewChild('canvas') private canvas!: ElementRef<HTMLCanvasElement>;
  private chart?: ChartJs;

  ngAfterViewInit(): void { this.render(); }
  ngOnChanges(_changes: SimpleChanges): void {
    if (!this.chart || !this.data) return;
    this.chart.destroy();
    this.render();
  }
  ngOnDestroy(): void { this.chart?.destroy(); }
  private render(): void {
    this.chart = new ChartJs(this.canvas.nativeElement, { type: this.type, data: this.data, options: this.options });
  }
}
