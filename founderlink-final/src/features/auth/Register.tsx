import React, { useState } from 'react';
import { useForm } from 'react-hook-form';
import { yupResolver } from '@hookform/resolvers/yup';
import { useNavigate, Link } from 'react-router-dom';
import { toast } from 'react-hot-toast';
import { Rocket, TrendingUp, Users, Eye, EyeOff, ArrowRight, ChevronLeft } from 'lucide-react';
import { register as registerApi } from '../../core/api/authApi';
import { registerSchema } from '../../shared/utils/validationSchemas';
import { RegisterFormData } from '../../types';

const SYNE = "'Syne', system-ui, sans-serif";
const DM   = "'DM Sans', system-ui, sans-serif";

const Logo: React.FC = () => (
  <div style={{
    width: 36, height: 36, borderRadius: 10,
    background: 'var(--brand)', boxShadow: '0 2px 8px rgba(5,150,105,0.35)',
    display: 'flex', alignItems: 'center', justifyContent: 'center',
    color: '#fff', fontWeight: 700, fontSize: 13, fontFamily: SYNE, flexShrink: 0,
  }}>FL</div>
);

interface RoleOption {
  value: RegisterFormData['role'];
  label: string;
  tagline: string;
  icon: React.ReactNode;
  color: string;
  bg: string;
  border: string;
}

const ROLES: RoleOption[] = [
  {
    value: 'ROLE_FOUNDER',
    label: 'Founder',
    tagline: 'Raise capital, manage your startup, build your team',
    icon: <Rocket size={20} />,
    color: 'var(--brand)',
    bg: 'var(--brand-subtle)',
    border: 'var(--brand-border)',
  },
  {
    value: 'ROLE_INVESTOR',
    label: 'Investor',
    tagline: 'Discover vetted startups and invest via Razorpay',
    icon: <TrendingUp size={20} />,
    color: 'var(--green)',
    bg: 'var(--green-bg)',
    border: 'rgba(22,163,74,0.25)',
  },
  {
    value: 'ROLE_COFOUNDER',
    label: 'Co-Founder',
    tagline: 'Browse open positions and join early-stage teams',
    icon: <Users size={20} />,
    color: 'var(--purple)',
    bg: 'var(--purple-bg)',
    border: 'rgba(124,58,237,0.25)',
  },
];

const Register: React.FC = () => {
  const {
    register, handleSubmit, setValue, watch,
    formState: { errors, isSubmitting },
  } = useForm<RegisterFormData>({ resolver: yupResolver(registerSchema) });
  const navigate = useNavigate();
  const selectedRole = watch('role');
  const [showPassword, setShowPassword] = useState(false);
  const [step, setStep] = useState<'role' | 'details'>('role');

  const activeRole = ROLES.find(r => r.value === selectedRole);

  const onSubmit = async (data: RegisterFormData): Promise<void> => {
    try {
      await registerApi(data);
      toast.success('Account created! Please sign in.');
      navigate('/login');
    } catch (err: any) {
      toast.error(err.response?.data?.message || 'Registration failed');
    }
  };

  return (
    <div style={{
      minHeight: '100vh', background: 'var(--bg)',
      position: 'relative', overflow: 'hidden',
      display: 'flex', flexDirection: 'column',
    }}>
      {/* Dot-grid matching landing page */}
      <div aria-hidden style={{
        position: 'absolute', inset: 0, zIndex: 0,
        backgroundImage: 'radial-gradient(circle, var(--border-medium) 1px, transparent 1px)',
        backgroundSize: '28px 28px',
        maskImage: 'radial-gradient(ellipse 90% 60% at 50% 30%, black 30%, transparent 100%)',
        WebkitMaskImage: 'radial-gradient(ellipse 90% 60% at 50% 30%, black 30%, transparent 100%)',
      }} />
      <div aria-hidden style={{
        position: 'absolute', top: 0, left: '50%', transform: 'translateX(-50%)',
        width: 700, height: 260, pointerEvents: 'none', zIndex: 0,
        background: 'radial-gradient(ellipse, rgba(5,150,105,0.05) 0%, transparent 70%)',
      }} />

      {/* Nav */}
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
          Have an account?{' '}
          <Link to="/login" style={{ color: 'var(--brand)', fontWeight: 600, textDecoration: 'none' }}>
            Sign in
          </Link>
        </p>
      </nav>

      {/* Content */}
      <div style={{
        flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center',
        padding: '16px 24px 64px', position: 'relative', zIndex: 1,
      }}>

        {/* ── STEP 1: Role picker ────────────────────────────────────── */}
        {step === 'role' && (
          <div style={{ width: '100%', maxWidth: 620 }}>

            {/* Eyebrow */}
            <div style={{
              display: 'inline-flex', alignItems: 'center', gap: 7,
              background: 'var(--brand-subtle)', border: '1px solid var(--brand-border)',
              borderRadius: 100, padding: '5px 13px', marginBottom: 20,
              fontSize: 11, fontWeight: 600, color: 'var(--brand)',
              letterSpacing: '0.06em', textTransform: 'uppercase', fontFamily: DM,
            }}>
              <span style={{ width: 5, height: 5, borderRadius: '50%', background: 'var(--brand)', flexShrink: 0 }} />
              Get started free
            </div>

            <h1 style={{
              fontFamily: SYNE, fontSize: 'clamp(28px, 4vw, 38px)', fontWeight: 700,
              color: 'var(--text-primary)', margin: '0 0 8px',
              letterSpacing: '-0.025em', lineHeight: 1.1,
            }}>
              How will you use<br />
              <span style={{ color: 'var(--brand)' }}>FounderLink?</span>
            </h1>
            <p style={{ fontFamily: DM, fontSize: 15, color: 'var(--text-secondary)', margin: '0 0 32px', lineHeight: 1.6 }}>
              Choose your role — this shapes your dashboard and features.
            </p>

            {/* Role cards */}
            <div style={{ display: 'flex', flexDirection: 'column', gap: 10, marginBottom: 24 }}>
              {ROLES.map((role) => {
                const isSelected = selectedRole === role.value;
                return (
                  <button
                    key={role.value}
                    type="button"
                    onClick={() => setValue('role', role.value)}
                    style={{
                      display: 'flex', alignItems: 'center', gap: 18,
                      padding: '18px 22px', borderRadius: 16, textAlign: 'left',
                      background: 'var(--surface)',
                      border: `1.5px solid ${isSelected ? role.color : 'var(--border)'}`,
                      cursor: 'pointer',
                      boxShadow: isSelected ? 'var(--shadow-hover)' : 'var(--shadow-card)',
                      transform: isSelected ? 'translateY(-1px)' : 'none',
                      transition: 'all 180ms ease',
                    }}
                    onMouseEnter={e => { if (!isSelected) { (e.currentTarget.style.borderColor = 'var(--border-medium)'); (e.currentTarget.style.boxShadow = 'var(--shadow-hover)'); } }}
                    onMouseLeave={e => { if (!isSelected) { (e.currentTarget.style.borderColor = 'var(--border)'); (e.currentTarget.style.boxShadow = 'var(--shadow-card)'); } }}
                  >
                    {/* Role icon */}
                    <div style={{
                      width: 48, height: 48, borderRadius: 12, flexShrink: 0,
                      background: isSelected ? role.bg : 'var(--surface-2)',
                      border: `1px solid ${isSelected ? role.border : 'var(--border)'}`,
                      display: 'flex', alignItems: 'center', justifyContent: 'center',
                      color: isSelected ? role.color : 'var(--text-muted)',
                      transition: 'all 180ms',
                    }}>
                      {role.icon}
                    </div>

                    {/* Text */}
                    <div style={{ flex: 1, minWidth: 0 }}>
                      <p style={{ fontFamily: SYNE, fontSize: 16, fontWeight: 600, color: 'var(--text-primary)', margin: '0 0 3px', letterSpacing: '-0.01em' }}>
                        {role.label}
                      </p>
                      <p style={{ fontFamily: DM, fontSize: 13, color: 'var(--text-secondary)', margin: 0, lineHeight: 1.4 }}>
                        {role.tagline}
                      </p>
                    </div>

                    {/* Radio circle */}
                    <div style={{
                      width: 20, height: 20, borderRadius: '50%', flexShrink: 0,
                      border: `2px solid ${isSelected ? role.color : 'var(--border-medium)'}`,
                      background: isSelected ? role.color : 'transparent',
                      display: 'flex', alignItems: 'center', justifyContent: 'center',
                      transition: 'all 180ms',
                    }}>
                      {isSelected && (
                        <div style={{ width: 7, height: 7, borderRadius: '50%', background: '#fff' }} />
                      )}
                    </div>
                  </button>
                );
              })}
            </div>

            <input type="hidden" {...register('role')} />
            {errors.role && (
              <p style={{ fontFamily: DM, fontSize: 12, color: 'var(--red)', marginBottom: 12 }}>{errors.role.message}</p>
            )}

            <button
              type="button"
              disabled={!selectedRole}
              onClick={() => selectedRole && setStep('details')}
              className="btn-primary"
              style={{ width: '100%', height: 48, fontSize: 15, gap: 8, justifyContent: 'center', opacity: selectedRole ? 1 : 0.45 }}
            >
              Continue as {activeRole?.label || '…'} <ArrowRight size={15} />
            </button>

            <p style={{ fontFamily: DM, fontSize: 14, color: 'var(--text-muted)', textAlign: 'center', marginTop: 20 }}>
              Already have an account?{' '}
              <Link to="/login" style={{ color: 'var(--brand)', fontWeight: 600, textDecoration: 'none' }}>Sign in</Link>
            </p>
          </div>
        )}

        {/* ── STEP 2: Account details ───────────────────────────────── */}
        {step === 'details' && (
          <div style={{ width: '100%', maxWidth: 440 }}>

            {/* Back + role badge row */}
            <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 28 }}>
              <button
                type="button"
                onClick={() => setStep('role')}
                style={{ background: 'var(--surface-2)', border: '1px solid var(--border)', borderRadius: 8, padding: '6px 10px', display: 'flex', alignItems: 'center', gap: 4, cursor: 'pointer', color: 'var(--text-muted)', fontFamily: DM, fontSize: 13 }}
              >
                <ChevronLeft size={14} /> Back
              </button>
              {activeRole && (
                <div style={{
                  display: 'inline-flex', alignItems: 'center', gap: 6,
                  background: activeRole.bg, border: `1px solid ${activeRole.border}`,
                  borderRadius: 8, padding: '5px 12px',
                  fontSize: 12, fontWeight: 600, color: activeRole.color, fontFamily: DM,
                }}>
                  {activeRole.icon}
                  {activeRole.label}
                </div>
              )}
            </div>

            {/* Eyebrow */}
            <div style={{
              display: 'inline-flex', alignItems: 'center', gap: 7,
              background: 'var(--brand-subtle)', border: '1px solid var(--brand-border)',
              borderRadius: 100, padding: '5px 13px', marginBottom: 18,
              fontSize: 11, fontWeight: 600, color: 'var(--brand)',
              letterSpacing: '0.06em', textTransform: 'uppercase', fontFamily: DM,
            }}>
              <span style={{ width: 5, height: 5, borderRadius: '50%', background: 'var(--brand)', flexShrink: 0 }} />
              Step 2 of 2
            </div>

            <h1 style={{
              fontFamily: SYNE, fontSize: 32, fontWeight: 700,
              color: 'var(--text-primary)', margin: '0 0 6px',
              letterSpacing: '-0.025em', lineHeight: 1.1,
            }}>
              Your details
            </h1>
            <p style={{ fontFamily: DM, fontSize: 15, color: 'var(--text-secondary)', margin: '0 0 28px', lineHeight: 1.6 }}>
              A few more details and you're in.
            </p>

            {/* Form card */}
            <div style={{
              background: 'var(--surface)', border: '1px solid var(--border)',
              borderRadius: 18, padding: '28px 28px 24px',
              boxShadow: 'var(--shadow-card)',
            }}>
              <form onSubmit={handleSubmit(onSubmit)} noValidate>
                <input type="hidden" {...register('role')} />

                {/* Name */}
                <div style={{ marginBottom: 16 }}>
                  <label style={{ display: 'block', fontFamily: DM, fontSize: 13, fontWeight: 600, color: 'var(--text-secondary)', marginBottom: 7 }}>
                    Full name
                  </label>
                  <input
                    placeholder="Arjun Sharma"
                    {...register('name')}
                    className="input-field"
                    style={{ border: errors.name ? '1px solid var(--red)' : undefined }}
                  />
                  {errors.name && <p style={{ fontFamily: DM, fontSize: 12, color: 'var(--red)', marginTop: 5 }}>{errors.name.message}</p>}
                </div>

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
                <div style={{ marginBottom: 24 }}>
                  <label style={{ display: 'block', fontFamily: DM, fontSize: 13, fontWeight: 600, color: 'var(--text-secondary)', marginBottom: 7 }}>
                    Password
                  </label>
                  <div style={{ position: 'relative' }}>
                    <input
                      type={showPassword ? 'text' : 'password'}
                      placeholder="At least 6 characters"
                      {...register('password')}
                      className="input-field"
                      style={{ paddingRight: 44, border: errors.password ? '1px solid var(--red)' : undefined }}
                    />
                    <button
                      type="button"
                      onClick={() => setShowPassword(v => !v)}
                      style={{ position: 'absolute', right: 12, top: '50%', transform: 'translateY(-50%)', background: 'none', border: 'none', cursor: 'pointer', color: 'var(--text-faint)', padding: 0, display: 'flex', alignItems: 'center' }}
                    >
                      {showPassword ? <EyeOff size={15} /> : <Eye size={15} />}
                    </button>
                  </div>
                  {errors.password && <p style={{ fontFamily: DM, fontSize: 12, color: 'var(--red)', marginTop: 5 }}>{errors.password.message}</p>}
                </div>

                <button
                  type="submit"
                  disabled={isSubmitting}
                  className="btn-primary"
                  style={{ width: '100%', height: 46, fontSize: 15, justifyContent: 'center' }}
                >
                  {isSubmitting ? 'Creating account…' : 'Create account'}
                </button>
              </form>
            </div>

            <p style={{ fontFamily: DM, fontSize: 14, color: 'var(--text-muted)', textAlign: 'center', marginTop: 20 }}>
              Already have an account?{' '}
              <Link to="/login" style={{ color: 'var(--brand)', fontWeight: 600, textDecoration: 'none' }}>Sign in</Link>
            </p>
          </div>
        )}
      </div>
    </div>
  );
};

export default Register;
