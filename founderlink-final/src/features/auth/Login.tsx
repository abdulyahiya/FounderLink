import React, { useState } from 'react';
import { useForm } from 'react-hook-form';
import { yupResolver } from '@hookform/resolvers/yup';
import { useDispatch } from 'react-redux';
import { useNavigate, Link } from 'react-router-dom';
import { toast } from 'react-hot-toast';
import { Eye, EyeOff, ArrowRight } from 'lucide-react';
import { setCredentials } from '../../store/slices/authSlice';
import { login } from '../../core/api/authApi';
import { loginSchema } from '../../shared/utils/validationSchemas';
import { LoginFormData } from '../../types';

const SYNE = "'Syne', system-ui, sans-serif";
const DM   = "'DM Sans', system-ui, sans-serif";

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

const Login: React.FC = () => {
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<LoginFormData>({
    resolver: yupResolver(loginSchema),
  });
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const [showPassword, setShowPassword] = useState(false);

  const onSubmit = async (data: LoginFormData): Promise<void> => {
    try {
      const res = await login(data);
      const { token, refreshToken, userId, role, email, name } = res.data;
      dispatch(setCredentials({ token, refreshToken, userId, role, email, name }));
      toast.success('Welcome back!');
      if (role === 'ROLE_FOUNDER')       navigate('/founder/dashboard');
      else if (role === 'ROLE_INVESTOR')  navigate('/investor/dashboard');
      else if (role === 'ROLE_COFOUNDER') navigate('/cofounder/dashboard');
      else if (role === 'ROLE_ADMIN')     navigate('/admin/dashboard');
      else navigate('/login');
    } catch (err: any) {
      toast.error(err.response?.data?.message || 'Invalid credentials');
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
      {/* Same dot-grid as landing page hero */}
      <div aria-hidden style={{
        position: 'absolute', inset: 0, zIndex: 0,
        backgroundImage: 'radial-gradient(circle, var(--border-medium) 1px, transparent 1px)',
        backgroundSize: '28px 28px',
        maskImage: 'radial-gradient(ellipse 80% 80% at 50% 40%, black 40%, transparent 100%)',
        WebkitMaskImage: 'radial-gradient(ellipse 80% 80% at 50% 40%, black 40%, transparent 100%)',
      }} />
      {/* Subtle brand glow at top center */}
      <div aria-hidden style={{
        position: 'absolute', top: 0, left: '50%', transform: 'translateX(-50%)',
        width: 600, height: 280, pointerEvents: 'none', zIndex: 0,
        background: 'radial-gradient(ellipse, rgba(5,150,105,0.06) 0%, transparent 70%)',
      }} />

      {/* Top nav — mirrors landing page nav */}
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
        <p style={{ fontFamily: DM, fontSize: 14, color: 'var(--text-muted)', margin: 0 }}>
          No account?{' '}
          <Link to="/register" style={{ color: 'var(--brand)', fontWeight: 600, textDecoration: 'none' }}>
            Sign up free
          </Link>
        </p>
      </nav>

      {/* Main centered content */}
      <div style={{
        flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center',
        padding: '24px 24px 64px', position: 'relative', zIndex: 1,
      }}>
        <div style={{ width: '100%', maxWidth: 420 }}>

          {/* Eyebrow */}
          <div style={{
            display: 'inline-flex', alignItems: 'center', gap: 7,
            background: 'var(--brand-subtle)', border: '1px solid var(--brand-border)',
            borderRadius: 100, padding: '5px 13px', marginBottom: 20,
            fontSize: 11, fontWeight: 600, color: 'var(--brand)',
            letterSpacing: '0.06em', textTransform: 'uppercase', fontFamily: DM,
          }}>
            <span style={{ width: 5, height: 5, borderRadius: '50%', background: 'var(--brand)', flexShrink: 0 }} />
            Welcome back
          </div>

          <h1 style={{
            fontFamily: SYNE, fontSize: 34, fontWeight: 700,
            color: 'var(--text-primary)', margin: '0 0 8px',
            letterSpacing: '-0.03em', lineHeight: 1.1,
          }}>
            Sign in to<br />
            <span style={{ color: 'var(--brand)' }}>FounderLink</span>
          </h1>
          <p style={{ fontFamily: DM, fontSize: 15, color: 'var(--text-secondary)', margin: '0 0 32px', lineHeight: 1.6 }}>
            Access your dashboard and pick up where you left off.
          </p>

          {/* Card */}
          <div style={{
            background: 'var(--surface)',
            border: '1px solid var(--border)',
            borderRadius: 18, padding: '28px 28px 24px',
            boxShadow: 'var(--shadow-card)',
          }}>
            <form onSubmit={handleSubmit(onSubmit)} noValidate>

              {/* Email */}
              <div style={{ marginBottom: 16 }}>
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

          

              

              {/* Password */}
              <div style={{ marginBottom: 22 }}>
                <label style={{ display: 'block', fontFamily: DM, fontSize: 13, fontWeight: 600, color: 'var(--text-secondary)', marginBottom: 7 }}>
                  Password
                </label>
                <div style={{ position: 'relative' }}>
                  <input
                    type={showPassword ? 'text' : 'password'}
                    placeholder="••••••••"
                    {...register('password')}
                    className="input-field"
                    style={{ paddingRight: 44, border: errors.password ? '1px solid var(--red)' : undefined }}
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
                  </button>
                </div>
                {errors.password && <p style={{ fontFamily: DM, fontSize: 12, color: 'var(--red)', marginTop: 5 }}>{errors.password.message}</p>}
                
                <div style={{ textAlign: 'right', marginTop: 8 }}>
                  <Link 
                    to="/forgot-password" 
                    style={{ 
                      fontFamily: DM, 
                      fontSize: 13, 
                      color: 'var(--brand)', 
                      textDecoration: 'none', 
                      fontWeight: 500 
                    }}
                  >
                    Forgot Password?
                  </Link>
                </div>
              </div>

              {/* Submit */}
              <button
                type="submit"
                disabled={isSubmitting}
                className="btn-primary"
                style={{ width: '100%', height: 46, fontSize: 15, gap: 8, justifyContent: 'center' }}
              >
                {isSubmitting ? 'Signing in…' : (
                  <>Continue <ArrowRight size={15} /></>
                )}
              </button>
            </form>
          </div>

          <p style={{ fontFamily: DM, fontSize: 14, color: 'var(--text-muted)', textAlign: 'center', marginTop: 20 }}>
            New to FounderLink?{' '}
            <Link to="/register" style={{ color: 'var(--brand)', fontWeight: 600, textDecoration: 'none' }}>
              Create an account
            </Link>
          </p>
        </div>
      </div>
    </div>
  );
};

export default Login;
