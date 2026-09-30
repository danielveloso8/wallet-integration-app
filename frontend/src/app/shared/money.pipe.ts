import { Pipe, PipeTransform } from '@angular/core';

@Pipe({ name: 'money', standalone: true })
export class MoneyPipe implements PipeTransform {
  transform(amount: string, currency: string): string {
    return new Intl.NumberFormat('pt-PT', { style: 'currency', currency }).format(Number(amount));
  }
}
