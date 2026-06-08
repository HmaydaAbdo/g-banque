import { Component, OnInit, AfterViewInit, OnDestroy, ElementRef, ViewChild, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { AuthService } from '../../services/auth.service';
import { LoginCredentials } from '../../dto/LoginCredentials';
import { InputTextModule } from 'primeng/inputtext';
import { PasswordModule } from 'primeng/password';
import { ButtonModule } from 'primeng/button';
import { InputGroupModule } from "primeng/inputgroup";
import { InputGroupAddonModule } from "primeng/inputgroupaddon";
import { MessageService } from 'primeng/api';
import { Router, RouterLink } from "@angular/router";
import { Role } from "../../enum/Role";


@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    CommonModule,
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
  private messageService = inject(MessageService);
  private animFrameId = 0;

  loginForm!: FormGroup;
  errorMessage: string | null = null;
  isLoading: boolean = false;

  ngOnInit(): void {
    this.loginForm = this.fb.group({
      username: ['', Validators.required],
      password: ['', Validators.required]
    });
  }

  login(): void {
    this.errorMessage = null;

    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      this.errorMessage = 'Veuillez remplir tous les champs obligatoires.';
      this.messageService.add({ severity: 'warn', summary: 'Attention', detail: 'Veuillez remplir tous les champs obligatoires.' });
      return;
    }

    this.isLoading = true;

    const credentials: LoginCredentials = this.loginForm.value;

    this.authService.login(credentials).subscribe({
      next: () => {
        this.isLoading = false;
        this.router.navigate(['/portail']);
      },
      error: (error) => {
        this.isLoading = false;
        this.errorMessage = 'Identifiants incorrects';
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Identifiants incorrects.' });
        console.error('Login error:', error);
      }
    });
  }

  ngAfterViewInit(): void {
    const canvas = this.canvasRef.nativeElement;
    const ctx = canvas.getContext('2d')!;
    let W = 0, H = 0;
    const resize = () => { W = canvas.width = window.innerWidth; H = canvas.height = window.innerHeight; };
    resize();
    window.addEventListener('resize', resize);
    const particles: any[] = [];
    for (let i = 0; i < 60; i++) particles.push({ x: Math.random() * 2000, y: Math.random() * 1000, size: Math.random() * 1.2 + 0.3, speed: Math.random() * 0.35 + 0.08, opacity: Math.random() * 0.5 + 0.1, drift: (Math.random() - 0.5) * 0.25 });
    const animate = () => {
      ctx.clearRect(0, 0, W, H);
      ctx.strokeStyle = 'rgba(99,102,241,0.06)'; ctx.lineWidth = 1;
      for (let x = 0; x < W; x += 80) { ctx.beginPath(); ctx.moveTo(x,0); ctx.lineTo(x,H); ctx.stroke(); }
      for (let y = 0; y < H; y += 80) { ctx.beginPath(); ctx.moveTo(0,y); ctx.lineTo(W,y); ctx.stroke(); }
      particles.forEach(p => {
        p.y -= p.speed; p.x += p.drift;
        if (p.y < -10) { p.y = H + 10; p.x = Math.random() * W; }
        ctx.beginPath(); ctx.arc(p.x, p.y, p.size, 0, Math.PI*2);
        ctx.fillStyle = `rgba(99,102,241,${p.opacity})`; ctx.fill();
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
