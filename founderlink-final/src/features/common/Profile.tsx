import React, { useEffect } from 'react';
import { AxiosError } from 'axios';
import { useForm, type Resolver } from 'react-hook-form';
import { yupResolver } from '@hookform/resolvers/yup';
import * as yup from 'yup';
import type { InferType } from 'yup';
import { toast } from 'react-hot-toast';
import { Mail, Shield } from 'lucide-react';
import Layout from '../../shared/components/Layout';
import Button from '../../shared/components/Button';
import useAuth from '../../shared/hooks/useAuth';
import { getMyProfile, updateProfile } from '../../core/api/userApi';
import { changePassword } from '../../core/api/authApi';
import { profileSchema } from '../../shared/utils/validationSchemas';
import { ProfileFormData, ChangePasswordData } from '../../types';

type ProfileFormValues = InferType<typeof profileSchema>;

interface ChangePasswordFormValues extends ChangePasswordData {
  confirmPassword: string;
}

const changePasswordSchema = yup.object().shape({
  oldPassword: yup.string().required('Current password is required'),
  newPassword: yup.string().min(6, 'Minimum 6 characters').required('New password is required'),
  confirmPassword: yup.string()
    .oneOf([yup.ref('newPassword')], 'Passwords must match')
    .required('Please confirm your password'),
});

const labelStyle: React.CSSProperties = {
  fontSize: 12, fontWeight: 600, color: 'var(--text-secondary)',
  display: 'block', marginBottom: 6, fontFamily: "'DM Sans', system-ui, sans-serif",
};

const Profile: React.FC = () => {
  const { user, userId } = useAuth();
  const { register, handleSubmit, reset, formState: { isSubmitting } } = useForm<ProfileFormValues>({
    resolver: yupResolver(profileSchema) as Resolver<ProfileFormValues>,
  });

  useEffect(() => {
    if (!userId) return;
    getMyProfile(userId).then((res) => reset(res.data)).catch(() => undefined);
  }, [reset, userId]);

  const onSubmit = async (data: ProfileFormValues): Promise<void> => {
    if (!userId) { toast.error('User not found'); return; }
    try {
      await updateProfile(userId, { ...data, email: user?.email } as ProfileFormData & { email?: string });
      toast.success('Profile updated!');
    } catch (error) {
      const err = error as AxiosError<{ message?: string }>;
      toast.error(err.response?.data?.message || 'Update failed');
    }
  };

  const roleLabel = user?.role?.replace('ROLE_', '') || 'User';
  const initial = user?.email?.[0]?.toUpperCase() || 'U';
  const roleColor = roleLabel === 'FOUNDER' ? 'var(--brand)' : roleLabel === 'INVESTOR' ? 'var(--green)' : roleLabel === 'COFOUNDER' ? 'var(--purple)' : 'var(--amber)';

  return (
    <Layout>
      <div style={{ maxWidth: 840, margin: '0 auto' }}>
        <div style={{ marginBottom: 28 }}>
          <span style={{ fontSize: 11, fontWeight: 600, letterSpacing: '0.1em', textTransform: 'uppercase', color: 'var(--brand)', display: 'block', marginBottom: 4 }}>Account</span>
          <h1 style={{ fontFamily: "'Syne', system-ui, sans-serif", fontSize: 28, fontWeight: 600, color: 'var(--text-primary)', margin: 0 }}>My profile</h1>
          <p style={{ color: 'var(--text-muted)', fontSize: 14, marginTop: 4 }}>Manage your public profile and account details</p>
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(260px, 1fr))', gap: 20, alignItems: 'start' }}>
          {/* Left — profile card */}
          <div style={{ background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: 16, padding: 24, boxShadow: 'var(--shadow-card)', textAlign: 'center' }}>
            <div style={{
              width: 72, height: 72, borderRadius: 20,
              background: `linear-gradient(135deg, ${roleColor}33, ${roleColor}22)`,
              border: `2px solid ${roleColor}44`,
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              margin: '0 auto 16px',
              fontSize: 28, fontWeight: 700, color: roleColor,
              fontFamily: "'Syne', system-ui, sans-serif",
            }}>
              {initial}
            </div>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 6, marginBottom: 8 }}>
              <Mail size={13} style={{ color: 'var(--text-faint)' }} />
              <p style={{ fontSize: 13, color: 'var(--text-secondary)', fontWeight: 500 }}>{user?.email}</p>
            </div>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 6 }}>
              <Shield size={12} style={{ color: 'var(--text-faint)' }} />
              <span style={{ fontSize: 11, fontWeight: 600, color: roleColor, background: `${roleColor}1a`, padding: '3px 10px', borderRadius: 20, border: `1px solid ${roleColor}33` }}>
                {roleLabel}
              </span>
            </div>
          </div>

          {/* Right — form */}
          <div style={{ background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: 16, padding: 28, boxShadow: 'var(--shadow-card)' }}>
            <form onSubmit={handleSubmit(onSubmit)} style={{ display: 'flex', flexDirection: 'column', gap: 18 }}>
              <div>
                <label style={labelStyle}>Full name</label>
                <input className="input-field" placeholder="Your full name" {...register('name')} />
              </div>
              <div>
                <label style={labelStyle}>Bio</label>
                <textarea
                  rows={3}
                  className="input-field"
                  style={{ height: 'auto', resize: 'vertical' }}
                  placeholder="Tell others a bit about yourself…"
                  {...register('bio')}
                />
              </div>
              <div>
                <label style={labelStyle}>Skills</label>
                <input className="input-field" placeholder="e.g. React, Java, Product Management" {...register('skills')} />
              </div>
              <div>
                <label style={labelStyle}>Experience</label>
                <textarea
                  rows={2}
                  className="input-field"
                  style={{ height: 'auto', resize: 'vertical' }}
                  placeholder="Your professional background…"
                  {...register('experience')}
                />
              </div>
              <div>
                <label style={labelStyle}>Portfolio / Links</label>
                <input className="input-field" placeholder="https://github.com/yourname" {...register('portfolioLinks')} />
              </div>
              <div style={{ display: 'flex', justifyContent: 'flex-end', paddingTop: 4 }}>
                <Button type="submit" variant="primary" isLoading={isSubmitting}>Save changes</Button>
              </div>
            </form>
          </div>
        </div>

        {/* Change Password Section */}
        <div style={{ marginTop: 40, maxWidth: 420 }}>
          <div style={{ marginBottom: 20 }}>
            <span style={{ fontSize: 11, fontWeight: 600, letterSpacing: '0.1em', textTransform: 'uppercase', color: 'var(--red)', display: 'block', marginBottom: 4 }}>Security</span>
            <h2 style={{ fontFamily: "'Syne', system-ui, sans-serif", fontSize: 22, fontWeight: 600, color: 'var(--text-primary)', margin: 0 }}>Account Security</h2>
            <p style={{ color: 'var(--text-muted)', fontSize: 14, marginTop: 4 }}>Manage your password and security settings</p>
          </div>

          <PasswordSettingsSection />
        </div>
      </div>
    </Layout>
  );
};

const PasswordSettingsSection: React.FC = () => {
  const [isOpen, setIsOpen] = React.useState(false);

  if (!isOpen) {
    return (
      <div style={{ 
        background: 'var(--surface)', 
        border: '1px solid var(--border)', 
        borderRadius: 16, 
        padding: '20px 24px', 
        display: 'flex', 
        alignItems: 'center', 
        justifyContent: 'space-between',
        boxShadow: 'var(--shadow-card)' 
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          <div style={{ width: 40, height: 40, borderRadius: 10, background: 'var(--red-bg)', display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'var(--red)' }}>
            <Shield size={20} />
          </div>
          <div>
            <p style={{ fontFamily: "'Syne', system-ui, sans-serif", fontWeight: 600, fontSize: 15, color: 'var(--text-primary)', margin: 0 }}>Password</p>
            <p style={{ fontSize: 12, color: 'var(--text-muted)', margin: 0 }}>Last updated recently</p>
          </div>
        </div>
        <Button variant="secondary" onClick={() => setIsOpen(true)} style={{ fontSize: 13, height: 36 }}>Update Password</Button>
      </div>
    );
  }

  return (
    <div style={{ background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: 16, padding: 28, boxShadow: 'var(--shadow-card)' }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 24 }}>
        <h3 style={{ fontFamily: "'Syne', system-ui, sans-serif", fontSize: 18, fontWeight: 600, color: 'var(--text-primary)', margin: 0 }}>Update Password</h3>
        <button 
          onClick={() => setIsOpen(false)}
          style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'var(--text-muted)', fontSize: 13, fontWeight: 500 }}
        >
          Cancel
        </button>
      </div>
      <ChangePasswordForm onSuccess={() => setIsOpen(false)} />
    </div>
  );
};

interface ChangePasswordFormProps {
  onSuccess?: () => void;
}

const ChangePasswordForm: React.FC<ChangePasswordFormProps> = ({ onSuccess }) => {
  const { register, handleSubmit, reset, formState: { errors, isSubmitting } } = useForm<ChangePasswordFormValues>({
    resolver: yupResolver(changePasswordSchema) as Resolver<ChangePasswordFormValues>,
  });

  const onSubmit = async (data: ChangePasswordFormValues) => {
    try {
      console.log('Attempting password change...');
      const response = await changePassword({ oldPassword: data.oldPassword, newPassword: data.newPassword });
      
      // If we reach here, it's a 2xx success
      console.log('Password change successful:', response.status);
      toast.success('Password updated successfully!');
      reset();
      if (onSuccess) onSuccess();
    } catch (error: any) {
      console.error('Password change error details:', error);
      const backendMessage = error.response?.data?.message;
      const message = backendMessage || 'Failed to update password. Please check your current password.';
      toast.error(message);
    }
  };

  return (
    <form onSubmit={handleSubmit(onSubmit)} style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
      <div>
        <label style={labelStyle}>Current Password</label>
        <input 
          type="password" 
          className="input-field" 
          placeholder="••••••••" 
          {...register('oldPassword')} 
          style={{ border: errors.oldPassword ? '1px solid var(--red)' : undefined }}
        />
        {errors.oldPassword && <p style={{ color: 'var(--red)', fontSize: 12, marginTop: 4 }}>{errors.oldPassword.message}</p>}
      </div>
      <div>
        <label style={labelStyle}>New Password</label>
        <input 
          type="password" 
          className="input-field" 
          placeholder="••••••••" 
          {...register('newPassword')} 
          style={{ border: errors.newPassword ? '1px solid var(--red)' : undefined }}
        />
        {errors.newPassword && <p style={{ color: 'var(--red)', fontSize: 12, marginTop: 4 }}>{errors.newPassword.message}</p>}
      </div>
      <div>
        <label style={labelStyle}>Confirm New Password</label>
        <input 
          type="password" 
          className="input-field" 
          placeholder="••••••••" 
          {...register('confirmPassword')} 
          style={{ border: errors.confirmPassword ? '1px solid var(--red)' : undefined }}
        />
        {errors.confirmPassword && <p style={{ color: 'var(--red)', fontSize: 12, marginTop: 4 }}>{errors.confirmPassword.message}</p>}
      </div>
      <div style={{ display: 'flex', justifyContent: 'flex-start', paddingTop: 8 }}>
        <Button 
          type="submit" 
          variant="primary" 
          isLoading={isSubmitting} 
          style={{ width: '100%', background: 'var(--red)', borderColor: 'var(--red)', height: 44 }}
        >
          Update Password
        </Button>
      </div>
    </form>
  );
};

export default Profile;
