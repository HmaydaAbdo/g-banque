import { Component, OnInit, OnDestroy, ElementRef, ViewChild, AfterViewInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-landing',
  standalone: true,
  imports: [RouterLink, CommonModule],
  templateUrl: './landing.component.html',
  styleUrl: './landing.component.scss'
})
export class LandingComponent implements AfterViewInit, OnDestroy {

  @ViewChild('canvas') canvasRef!: ElementRef<HTMLCanvasElement>;

  private animFrameId: number = 0;
  private particles: any[] = [];

  ngAfterViewInit(): void {
    this.initCanvas();
    this.initScrollReveal();
  }

  ngOnDestroy(): void {
    cancelAnimationFrame(this.animFrameId);
  }

  private initCanvas(): void {
    const canvas = this.canvasRef.nativeElement;
    const ctx = canvas.getContext('2d')!;
    let W = 0, H = 0;

    const resize = () => {
      W = canvas.width = window.innerWidth;
      H = canvas.height = window.innerHeight;
    };
    resize();
    window.addEventListener('resize', resize);

    class Particle {
      x = 0; y = 0; size = 0; speed = 0; opacity = 0; drift = 0;
      constructor() { this.reset(true, W, H); }
      reset(init: boolean, w: number, h: number) {
        this.x = Math.random() * w;
        this.y = init ? Math.random() * h : h + 10;
        this.size = Math.random() * 1.2 + 0.3;
        this.speed = Math.random() * 0.4 + 0.1;
        this.opacity = Math.random() * 0.6 + 0.1;
        this.drift = (Math.random() - 0.5) * 0.3;
      }
      update(w: number, h: number) {
        this.y -= this.speed; this.x += this.drift;
        if (this.y < -10) this.reset(false, w, h);
      }
      draw(ctx: CanvasRenderingContext2D) {
        ctx.beginPath();
        ctx.arc(this.x, this.y, this.size, 0, Math.PI * 2);
        ctx.fillStyle = `rgba(99,102,241,${this.opacity})`;
        ctx.fill();
      }
    }

    for (let i = 0; i < 80; i++) this.particles.push(new Particle());

    const drawGrid = () => {
      ctx.clearRect(0, 0, W, H);
      ctx.strokeStyle = 'rgba(99,102,241,0.06)';
      ctx.lineWidth = 1;
      const step = 80;
      for (let x = 0; x < W; x += step) {
        ctx.beginPath(); ctx.moveTo(x, 0); ctx.lineTo(x, H); ctx.stroke();
      }
      for (let y = 0; y < H; y += step) {
        ctx.beginPath(); ctx.moveTo(0, y); ctx.lineTo(W, y); ctx.stroke();
      }
    };

    const animate = () => {
      drawGrid();
      this.particles.forEach((p: any) => { p.update(W, H); p.draw(ctx); });
      this.animFrameId = requestAnimationFrame(animate);
    };
    animate();
  }

  private initScrollReveal(): void {
    const observer = new IntersectionObserver(entries => {
      entries.forEach((e, i) => {
        if (e.isIntersecting) {
          setTimeout(() => e.target.classList.add('visible'), i * 80);
          observer.unobserve(e.target);
        }
      });
    }, { threshold: 0.12 });

    setTimeout(() => {
      document.querySelectorAll('.reveal').forEach(el => observer.observe(el));
    }, 100);
  }
}
