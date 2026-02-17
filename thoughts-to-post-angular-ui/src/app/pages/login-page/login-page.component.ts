import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-login-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  template: `
    <div class="auth-container fade-in">
      <div class="card auth-card">
        <h2 class="text-center">Login</h2>
        <form [formGroup]="loginForm" (ngSubmit)="onSubmit()">
          <div class="form-group">
            <label class="form-label" for="email">Email</label>
            <input type="email" id="email" formControlName="email" class="form-input" placeholder="Enter your email">
            <div *ngIf="loginForm.get('email')?.touched && loginForm.get('email')?.invalid" class="error-message">
              Valid email is required.
            </div>
          </div>
          <div class="form-group">
            <label class="form-label" for="password">Password</label>
            <input type="password" id="password" formControlName="password" class="form-input" placeholder="Enter your password">
            <div *ngIf="loginForm.get('password')?.touched && loginForm.get('password')?.invalid" class="error-message">
              Password is required.
            </div>
          </div>
          <div *ngIf="errorMessage" class="api-error">
            {{ errorMessage }}
          </div>
          <button type="submit" class="btn btn-primary btn-block" [disabled]="loginForm.invalid || isLoading">
            {{ isLoading ? 'Logging in...' : 'Login' }}
          </button>
        </form>
        <p class="auth-footer">
          Don't have an account? <a routerLink="/register">Register here</a>
        </p>
      </div>
    </div>
  `,
  styles: [`
    .auth-container {
      display: flex;
      justify-content: center;
      align-items: center;
      min-height: calc(100vh - 200px);
      padding: var(--spacing-lg);
    }
    .auth-card {
      width: 100%;
      max-width: 450px;
    }
    .btn-block {
      width: 100%;
      margin-top: var(--spacing-md);
    }
    .error-message {
      color: #f45c43;
      font-size: 0.875rem;
      margin-top: 0.25rem;
    }
    .api-error {
      background: var(--error-gradient);
      color: white;
      padding: var(--spacing-md);
      border-radius: var(--radius-md);
      margin-bottom: var(--spacing-md);
      text-align: center;
      font-size: 0.875rem;
    }
    .auth-footer {
      margin-top: var(--spacing-xl);
      text-align: center;
      font-size: 0.875rem;
      color: var(--text-secondary);
    }
    .auth-footer a {
      color: #667eea;
      font-weight: 500;
      text-decoration: underline;
    }
  `]
})
export class LoginPageComponent {
  loginForm: FormGroup;
  isLoading = false;
  errorMessage = '';

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) {
    this.loginForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', Validators.required]
    });
  }

  onSubmit(): void {
    if (this.loginForm.valid) {
      this.isLoading = true;
      this.errorMessage = '';
      this.authService.login(this.loginForm.value).subscribe({
        next: () => {
          this.router.navigate(['/thoughts']);
        },
        error: (err) => {
          this.isLoading = false;
          this.errorMessage = 'Invalid email or password';
          console.error('Login failed', err);
        }
      });
    }
  }
}
