import { Component, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { ApiService } from '../../core/api.service';
import { Channel, Program } from '../../models';

@Component({
  selector: 'app-radio',
  templateUrl: './radio.html',
  styleUrl: './radio.scss'
})
export class Radio implements OnInit, OnDestroy {
  private readonly api = inject(ApiService);

  protected readonly channels = signal<Channel[]>([]);
  protected readonly allPrograms = signal<Program[]>([]);
  protected readonly selected = signal<Channel | null>(null);
  protected readonly playing = signal<Channel | null>(null);
  protected readonly volume = signal(80);
  protected readonly streamError = signal('');
  protected readonly now = signal(new Date());

  private audio: HTMLAudioElement | null = null;
  private clock?: ReturnType<typeof setInterval>;

  ngOnInit(): void {
    this.api.channels().subscribe((cs) => {
      this.channels.set(cs);
      if (cs.length > 0) {
        this.pick(cs[0]);
      }
    });
    this.api.programs().subscribe((p) => this.allPrograms.set(p));
    this.clock = setInterval(() => this.now.set(new Date()), 30000);
  }

  ngOnDestroy(): void {
    this.stopAudio();
    clearInterval(this.clock);
  }

  pick(channel: Channel): void {
    this.selected.set(channel);
  }

  programsOf(channelId: number): Program[] {
    return this.allPrograms().filter((p) => p.channelId === channelId);
  }

  currentProgram(channelId: number): Program | null {
    return this.programsOf(channelId).find((p) => this.isNow(p.timeSlot)) ?? null;
  }

  isNow(slot: string): boolean {
    if (slot === '整點') {
      return true;
    }
    const m = slot.match(/(\d{2}):(\d{2})-(\d{2}):(\d{2})/);
    if (!m) {
      return false;
    }
    const n = this.now();
    const cur = n.getHours() * 60 + n.getMinutes();
    const start = +m[1] * 60 + +m[2];
    const end = +m[3] * 60 + +m[4];
    return cur >= start && cur < end;
  }

  togglePlay(channel: Channel): void {
    if (this.playing()?.id === channel.id) {
      this.stopAudio();
      this.playing.set(null);
      return;
    }
    this.streamError.set('');
    this.stopAudio();
    const audio = new Audio(channel.streamUrl);
    audio.loop = true;
    audio.volume = this.volume() / 100;
    audio.onerror = () => {
      this.streamError.set(`「${channel.name}」串流暫時無法使用`);
      this.playing.set(null);
    };
    this.audio = audio;
    audio
      .play()
      .then(() => this.playing.set(channel))
      .catch(() => {
        this.streamError.set('串流啟動失敗，請再試一次');
        this.playing.set(null);
      });
  }

  setVolume(value: number): void {
    this.volume.set(value);
    if (this.audio) {
      this.audio.volume = value / 100;
    }
  }

  private stopAudio(): void {
    this.audio?.pause();
    this.audio = null;
  }
}
