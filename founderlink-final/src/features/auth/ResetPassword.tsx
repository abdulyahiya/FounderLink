import React, { useState } from 'react';
import { useForm } from 'react-hook-form';
import { yupResolver } from '@hookform/resolvers/yup';
import { useNavigate, useSearchParams, Link } from 'react-router-dom';
import { toast } from 'react-hot-toast';
import { ArrowRight, Lock, Eye, EyeOff } from 'lucide-react';
import * as yup from 'yup';
import { resetPassword } from '../../core/api/authApi';
import { ResetPasswordData } from '../../types';

const SYNE = "'Syne', system-ui, sans-serif";
const DM   = "'DM Sans', system-ui, sans-serif";

const resetPasswordSchema = yup.object().shape({
  newPassword: yup.string().min(6, 'Password must be at least 6 characters').required('New password is required'),
  confirmPassword: yup.string()
    .oneOf([yup.ref('newPassword')], 'Passwords must match')
    .required('Please confirm your password'),
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

const ResetPassword: React.FC = () => {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token');
  const navigate = useNavigate();
  const [showPassword, setShowPassword] = useState(false);

  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<{ newPassword: string; confirmPassword: string }>({
    resolver: yupResolver(resetPasswordSchema),
  });

  const onSubmit = async (data: any): Promise<void> => {
    if (!token) {
      toast.error('Invalid reset token');
      return;
    }
    try {
      await resetPassword({ token, newPassword: data.newPassword });
      toast.success('Password reset successfully!');
      navigate('/login');
    } catch (err: any) {
      toast.error(err.response?.data?.message || 'Failed to reset password');
    }
  };

  if (!token) {
    return (
      <div style={{ textAlign: 'center', padding: 50, fontFamily: DM }}>
        <h2 style={{ color: 'var(--red)' }}>Invalid or missing token.</h2>
        <Link to="/login" style={{ color: 'var(--brand)' }}>Go back to login</Link>
      </div>
    );
  }

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
            <Lock size={12} />
            Secure Reset
          </div>

          <h1 style={{
            fontFamily: SYNE, fontSize: 34, fontWeight: 700,
            color: 'var(--text-primary)', margin: '0 0 8px',
            letterSpacing: '-0.03em', lineHeight: 1.1,
          }}>
            Set a new<br />
            <span style={{ color: 'var(--brand)' }}>Password</span>
          </h1>
          <p style={{ fontFamily: DM, fontSize: 15, color: 'var(--text-secondary)', margin: '0 0 32px', lineHeight: 1.6 }}>
            Please enter and confirm your new secure password.
          </p>

          <div style={{
            background: 'var(--surface)',
            border: '1px solid var(--border)',
            borderRadius: 18, padding: '28px 28px 24px',
            boxShadow: 'var(--shadow-card)',
          }}>
            <form onSubmit={handleSubmit(onSubmit)} noValidate>
              
              <div style={{ marginBottom: 16 }}>
                <label style={{ display: 'block', fontFamily: DM, fontSize: 13, fontWeight: 600, color: 'var(--text-secondary)', marginBottom: 7 }}>
                  New Password
                </label>
                <div style={{ position: 'relative' }}>
                  <input
                    type={showPassword ? 'text' : 'password'}
                    placeholder="••••••••"
                    {...register('newPassword')}
                    className="input-field"
                    style={{ paddingRight: 44, border: errors.newPassword ? '1px solid var(--red)' : undefined }}
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword(v => !v)}
                    style={{
                      position: 'absolute', right: 12, top: '50%', transform: 'translateY(-50%)',
                      background: 'none', border: 'none', cursor: 'pointer',
                      color: 'var(--text-faint)', padding: 0, display: 'flex', alignItems: 'center',
                    }}
                  >
                    {showPassword ? <EyeOff size={15} /> : <Eye size={15} />}
                  </button>
                </div>
                {errors.newPassword && <p style={{ fontFamily: DM, fontSize: 12, color: 'var(--red)', marginTop: 5 }}>{errors.newPassword.message}</p>}
              </div>

              <div style={{ marginBottom: 24 }}>
                <label style={{ display: 'block', fontFamily: DM, fontSize: 13, fontWeight: 600, color: 'var(--text-secondary)', marginBottom: 7 }}>
                  Confirm New Password
                </label>
                <input
                  type="password"
                  placeholder="••••••••"
                  {...register('confirmPassword')}
                  className="input-field"
                  style={{ border: errors.confirmPassword ? '1px solid var(--red)' : undefined }}
                />
                {errors.confirmPassword && <p style={{ fontFamily: DM, fontSize: 12, color: 'var(--red)', marginTop: 5 }}>{errors.confirmPassword.message}</p>}
              </div>

              <button
                type="submit"
                disabled={isSubmitting}
                className="btn-primary"
                style={{ width: '100%', height: 46, fontSize: 15, gap: 8, justifyContent: 'center' }}
              >
                {isSubmitting ? 'Resetting...' : (
                  <>Update Password <ArrowRight size={15} /></>
                )}
              </button>
            </form>
          </div>
        </div>
      </div>
    </div>
  );
};

export default ResetPassword;
