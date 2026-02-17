import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-register-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  template: `
    <div class="auth-container fade-in">
      <div class="card auth-card">
        <h2 class="text-center">Register</h2>
        <form [formGroup]="registerForm" (ngSubmit)="onSubmit()">
          <div class="form-group">
            <label class="form-label" for="email">Email</label>
            <input type="email" id="email" formControlName="email" class="form-input" placeholder="Enter your email">
            <div *ngIf="registerForm.get('email')?.touched && registerForm.get('email')?.invalid" class="error-message">
              Valid email is required.
            </div>
          </div>
          <div class="form-group">
            <label class="form-label" for="password">Password</label>
            <input type="password" id="password" formControlName="password" class="form-input" placeholder="Enter password">
            <div *ngIf="registerForm.get('password')?.touched && registerForm.get('password')?.invalid" class="error-message">
              <div *ngIf="registerForm.get('password')?.errors?.['required']">Password is required.</div>
              <div *ngIf="registerForm.get('password')?.errors?.['minlength']">Minimum 8 characters.</div>
              <div *ngIf="registerForm.get('password')?.errors?.['pattern']">Must contain at least one letter, one number, and one special character.</div>
            </div>
          </div>
          <div class="form-group">
            <label class="form-label" for="confirmPassword">Confirm Password</label>
            <input type="password" id="confirmPassword" formControlName="confirmPassword" class="form-input" placeholder="Confirm password">
            <div *ngIf="registerForm.get('confirmPassword')?.touched && registerForm.hasError('mismatch')" class="error-message">
              Passwords do not match.
            </div>
          </div>
          <div *ngIf="errorMessage" class="api-error">
            {{ errorMessage }}
          </div>
          <div *ngIf="successMessage" class="api-success">
            {{ successMessage }}
          </div>
          <button type="submit" class="btn btn-primary btn-block" [disabled]="registerForm.invalid || isLoading">
            {{ isLoading ? 'Registering...' : 'Register' }}
          </button>
        </form>
        <p class="auth-footer">
          Already have an account? <a routerLink="/login">Login here</a>
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
    .api-success {
      background: var(--success-gradient);
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
export class RegisterPageComponent {
  registerForm: FormGroup;
  isLoading = false;
  errorMessage = '';
  successMessage = '';

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) {
    this.registerForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [
        Validators.required,
        Validators.minLength(8),
        Validators.pattern(/^(?=.*[A-Za-z])(?=.*\d)(?=.*[@$!%*#?&])[A-Za-z\d@$!%*#?&]{8,}$/)
      ]],
      confirmPassword: ['', Validators.required]
    }, { validators: this.passwordMatchValidator });
  }

  passwordMatchValidator(g: FormGroup) {
    return g.get('password')?.value === g.get('confirmPassword')?.value
      ? null : { 'mismatch': true };
  }

  onSubmit(): void {
    if (this.registerForm.valid) {
      this.isLoading = true;
      this.errorMessage = '';
      this.successMessage = '';

      const { email, password } = this.registerForm.value;

      this.authService.register({ email, password }).subscribe({
        next: () => {
          this.isLoading = false;
          this.successMessage = 'Registration successful! Redirecting to login...';
          setTimeout(() => {
            this.router.navigate(['/login']);
          }, 2000);
        },
        error: (err) => {
          this.isLoading = false;
          this.errorMessage = err.error || 'Registration failed';
          console.error('Registration failed', err);
        }
      });
    }
  }
}
