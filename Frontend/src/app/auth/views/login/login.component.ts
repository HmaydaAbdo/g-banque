import { Component, OnInit, AfterViewInit, OnDestroy, ElementRef, ViewChild, inject } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { AuthService } from '../../services/auth.service';
import { LoginCredentials } from '../../dto/LoginCredentials';
import { InputTextModule } from 'primeng/inputtext';
import { PasswordModule } from 'primeng/password';
import { ButtonModule } from 'primeng/button';
import { InputGroupModule } from "primeng/inputgroup";
import { InputGroupAddonModule } from "primeng/inputgroupaddon";
import { Router, RouterLink } from "@angular/router";


@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    InputTextModule,
    PasswordModule,
    ButtonModule,
    InputGroupModule,
    InputGroupAddonModule,
    RouterLink,
  ],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss'],
})
export class LoginComponent implements OnInit, AfterViewInit, OnDestroy {

  @ViewChild('canvas') canvasRef!: ElementRef<HTMLCanvasElement>;

  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);
  passwordVisible: boolean = false;
  passwordInputType: string = 'password';
  private animFrameId = 0;

  loginForm!: FormGroup;
  isLoading: boolean = false;

  ngOnInit(): void {
    this.loginForm = this.fb.group({
      username: ['', Validators.required],
      password: ['', Validators.required]
    });
  }

  login(): void {
    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }

    this.isLoading = true;

    const credentials: LoginCredentials = this.loginForm.value;

    this.authService.login(credentials).subscribe({
      next: () => {
        this.isLoading = false;
        this.router.navigate(['/portail']);
      },
      error: () => {
        // Toast d'erreur géré de façon centralisée par l'intercepteur HTTP.
        // Le callback error est uniquement là pour stopper l'indicateur de chargement.
        this.isLoading = false;
      }
    });
  }

  ngAfterViewInit(): void {
    const canvas = this.canvasRef.nativeElement;
    const ctx = canvas.getContext('2d')!;
    // Canvas2D ne comprend pas var(--token) — on résout la valeur du design token
    // une seule fois ici (le canvas API a besoin d'une chaîne de couleur littérale).
    const accentRgb = getComputedStyle(document.documentElement).getPropertyValue('--color-accent-rgb').trim();
    let W = 0, H = 0;
    const resize = () => { W = canvas.width = window.innerWidth; H = canvas.height = window.innerHeight; };
    resize();
    window.addEventListener('resize', resize);
    const particles: any[] = [];
    for (let i = 0; i < 60; i++) particles.push({ x: Math.random() * 2000, y: Math.random() * 1000, size: Math.random() * 1.2 + 0.3, speed: Math.random() * 0.35 + 0.08, opacity: Math.random() * 0.5 + 0.1, drift: (Math.random() - 0.5) * 0.25 });
    const animate = () => {
      ctx.clearRect(0, 0, W, H);
      ctx.strokeStyle = `rgba(${accentRgb}, 0.06)`; ctx.lineWidth = 1;
      for (let x = 0; x < W; x += 80) { ctx.beginPath(); ctx.moveTo(x,0); ctx.lineTo(x,H); ctx.stroke(); }
      for (let y = 0; y < H; y += 80) { ctx.beginPath(); ctx.moveTo(0,y); ctx.lineTo(W,y); ctx.stroke(); }
      particles.forEach(p => {
        p.y -= p.speed; p.x += p.drift;
        if (p.y < -10) { p.y = H + 10; p.x = Math.random() * W; }
        ctx.beginPath(); ctx.arc(p.x, p.y, p.size, 0, Math.PI*2);
        ctx.fillStyle = `rgba(${accentRgb}, ${p.opacity})`; ctx.fill();
      });
      this.animFrameId = requestAnimationFrame(animate);
    };
    animate();
  }

  ngOnDestroy(): void { cancelAnimationFrame(this.animFrameId); }

  togglePasswordVisibility(): void {
    this.passwordVisible = !this.passwordVisible;
    this.passwordInputType = this.passwordVisible ? 'text' : 'password';
  }
}
