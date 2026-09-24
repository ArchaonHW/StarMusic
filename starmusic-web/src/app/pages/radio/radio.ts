import { Component, inject, OnInit, signal } from '@angular/core';
import { ApiService } from '../../core/api.service';
import { Channel, Program } from '../../models';

@Component({
  selector: 'app-radio',
  templateUrl: './radio.html',
  styleUrl: './radio.scss'
})
export class Radio implements OnInit {
  private readonly api = inject(ApiService);

  protected readonly channels = signal<Channel[]>([]);
  protected readonly programs = signal<Program[]>([]);
  protected readonly selected = signal<Channel | null>(null);
  protected readonly playing = signal<Channel | null>(null);

  ngOnInit(): void {
    this.api.channels().subscribe((cs) => {
      this.channels.set(cs);
      if (cs.length > 0) {
        this.pick(cs[0]);
      }
    });
  }

  pick(channel: Channel): void {
    this.selected.set(channel);
    this.api.programs(channel.id).subscribe((p) => this.programs.set(p));
  }

  togglePlay(channel: Channel): void {
    this.playing.set(this.playing()?.id === channel.id ? null : channel);
  }
}
