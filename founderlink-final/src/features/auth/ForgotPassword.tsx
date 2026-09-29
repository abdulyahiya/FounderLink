import React from 'react';
import { useForm } from 'react-hook-form';
import { yupResolver } from '@hookform/resolvers/yup';
import { Link } from 'react-router-dom';
import { toast } from 'react-hot-toast';
import { ArrowRight, Mail } from 'lucide-react';
import * as yup from 'yup';
import { forgotPassword } from '../../core/api/authApi';
import { ForgotPasswordData } from '../../types';

const SYNE = "'Syne', system-ui, sans-serif";
const DM   = "'DM Sans', system-ui, sans-serif";

const forgotPasswordSchema = yup.object().shape({
  email: yup.string().email('Invalid email').required('Email is required'),
});

const Logo: React.FC = () => (
  <div style={{
    width: 36, height: 36, borderRadius: 10,
    background: 'var(--brand)',
    boxShadow: '0 2px 8px rgba(5,150,105,0.35)',
    display: 'flex', alignItems: 'center', justifyContent: 'center',
    color: '#fff', fontWeight: 700, fontSize: 13, fontFamily: SYNE,
    flexShrink: 0,
  }}>FL</div>
);

const ForgotPassword: React.FC = () => {
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<ForgotPasswordData>({
    resolver: yupResolver(forgotPasswordSchema),
  });

  const onSubmit = async (data: ForgotPasswordData): Promise<void> => {
    try {
      await forgotPassword(data);
      toast.success('Reset link sent! Check your email.');
    } catch (err: any) {
      toast.error(err.response?.data?.message || 'Failed to send reset link');
    }
  };

  return (
    <div style={{
      minHeight: '100vh',
      background: 'var(--bg)',
      position: 'relative',
      overflow: 'hidden',
      display: 'flex',
      flexDirection: 'column',
    }}>
      <div aria-hidden style={{
        position: 'absolute', inset: 0, zIndex: 0,
        backgroundImage: 'radial-gradient(circle, var(--border-medium) 1px, transparent 1px)',
        backgroundSize: '28px 28px',
        maskImage: 'radial-gradient(ellipse 80% 80% at 50% 40%, black 40%, transparent 100%)',
        WebkitMaskImage: 'radial-gradient(ellipse 80% 80% at 50% 40%, black 40%, transparent 100%)',
      }} />

      <nav style={{
        position: 'relative', zIndex: 10,
        padding: '16px 32px',
        display: 'flex', alignItems: 'center', justifyContent: 'space-between',
      }}>
        <Link to="/" style={{ display: 'flex', alignItems: 'center', gap: 10, textDecoration: 'none' }}>
          <Logo />
          <span style={{ fontFamily: SYNE, fontSize: 17, fontWeight: 700, color: 'var(--text-primary)', letterSpacing: '-0.01em' }}>
            FounderLink
          </span>
        </Link>
      </nav>

      <div style={{
        flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center',
        padding: '24px 24px 64px', position: 'relative', zIndex: 1,
      }}>
        <div style={{ width: '100%', maxWidth: 420 }}>
          
          <div style={{
            display: 'inline-flex', alignItems: 'center', gap: 7,
            background: 'var(--brand-subtle)', border: '1px solid var(--brand-border)',
            borderRadius: 100, padding: '5px 13px', marginBottom: 20,
            fontSize: 11, fontWeight: 600, color: 'var(--brand)',
            letterSpacing: '0.06em', textTransform: 'uppercase', fontFamily: DM,
          }}>
            <Mail size={12} />
            Password Recovery
          </div>

          <h1 style={{
            fontFamily: SYNE, fontSize: 34, fontWeight: 700,
            color: 'var(--text-primary)', margin: '0 0 8px',
            letterSpacing: '-0.03em', lineHeight: 1.1,
          }}>
            Forgot your<br />
            <span style={{ color: 'var(--brand)' }}>Password?</span>
          </h1>
          <p style={{ fontFamily: DM, fontSize: 15, color: 'var(--text-secondary)', margin: '0 0 32px', lineHeight: 1.6 }}>
            No worries! Enter your email and we'll send you a link to reset it.
          </p>

          <div style={{
            background: 'var(--surface)',
            border: '1px solid var(--border)',
            borderRadius: 18, padding: '28px 28px 24px',
            boxShadow: 'var(--shadow-card)',
          }}>
            <form onSubmit={handleSubmit(onSubmit)} noValidate>
              <div style={{ marginBottom: 24 }}>
                <label style={{ display: 'block', fontFamily: DM, fontSize: 13, fontWeight: 600, color: 'var(--text-secondary)', marginBottom: 7 }}>
                  Email address
                </label>
                <input
                  type="email"
                  placeholder="you@example.com"
                  {...register('email')}
                  className="input-field"
                  style={{ border: errors.email ? '1px solid var(--red)' : undefined }}
                />
                {errors.email && <p style={{ fontFamily: DM, fontSize: 12, color: 'var(--red)', marginTop: 5 }}>{errors.email.message}</p>}
              </div>

              <button
                type="submit"
                disabled={isSubmitting}
                className="btn-primary"
                style={{ width: '100%', height: 46, fontSize: 15, gap: 8, justifyContent: 'center' }}
              >
                {isSubmitting ? 'Sending Link...' : (
                  <>Send Reset Link <ArrowRight size={15} /></>
                )}
              </button>
            </form>
          </div>

          <p style={{ fontFamily: DM, fontSize: 14, color: 'var(--text-muted)', textAlign: 'center', marginTop: 20 }}>
            Remembered it?{' '}
            <Link to="/login" style={{ color: 'var(--brand)', fontWeight: 600, textDecoration: 'none' }}>
              Back to Login
            </Link>
          </p>
        </div>
      </div>
    </div>
  );
};

export default ForgotPassword;
