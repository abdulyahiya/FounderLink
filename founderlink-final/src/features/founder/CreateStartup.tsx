import React, { useState } from 'react';
import { useForm } from 'react-hook-form';
import { yupResolver } from '@hookform/resolvers/yup';
import { useNavigate } from 'react-router-dom';
import { toast } from 'react-hot-toast';
import { Rocket } from 'lucide-react';
import Layout from '../../shared/components/Layout';
import Button from '../../shared/components/Button';
import { createStartup } from '../../core/api/startupApi';
import { startupSchema } from '../../shared/utils/validationSchemas';
import { StartupFormData } from '../../types';

const labelStyle: React.CSSProperties = {
  fontSize: 12, fontWeight: 600, color: 'var(--text-secondary)',
  display: 'block', marginBottom: 6, fontFamily: "'DM Sans', system-ui, sans-serif",
};
const errStyle: React.CSSProperties = { fontSize: 11, color: 'var(--red)', marginTop: 4 };

const sectionHeading = (title: string) => (
  <div style={{ fontSize: 11, fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.08em', color: 'var(--text-faint)', paddingBottom: 12, borderBottom: '1px solid var(--border)', marginBottom: 16 }}>
    {title}
  </div>
);

const CreateStartup: React.FC = () => {
  const { register, handleSubmit, watch, formState: { errors, isSubmitting } } = useForm<StartupFormData>({ resolver: yupResolver(startupSchema) });
  const navigate = useNavigate();
  const watched = watch();

  const onSubmit = async (data: StartupFormData): Promise<void> => {
    try {
      await createStartup({ ...data, fundingGoal: parseFloat(String(data.fundingGoal)) });
      toast.success('Startup created successfully!');
      navigate('/founder/startups');
    } catch (err: any) {
      toast.error(err.response?.data?.message || 'Failed to create startup');
    }
  };

  return (
    <Layout>
      <div style={{ maxWidth: 1040, margin: '0 auto' }}>
        <div style={{ marginBottom: 28 }}>
          <span style={{ fontSize: 11, fontWeight: 600, letterSpacing: '0.1em', textTransform: 'uppercase', color: 'var(--brand)', display: 'block', marginBottom: 4 }}>Founder</span>
          <h1 style={{ fontFamily: "'Syne', system-ui, sans-serif", fontSize: 28, fontWeight: 600, color: 'var(--text-primary)', margin: 0 }}>Create startup</h1>
          <p style={{ color: 'var(--text-muted)', fontSize: 14, marginTop: 4 }}>Fill in the details below to submit for admin review</p>
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: 20, alignItems: 'start' }}>
          {/* Left — form */}
          <div style={{ background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: 16, padding: 28, boxShadow: 'var(--shadow-card)' }}>
            <form onSubmit={handleSubmit(onSubmit)} style={{ display: 'flex', flexDirection: 'column', gap: 18 }}>
              {sectionHeading('Basic info')}
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: 14 }}>
                <div>
                  <label style={labelStyle}>Startup name</label>
                  <input className="input-field" placeholder="e.g. GreenTech" {...register('name', { required: 'Required' })} />
                  {errors.name && <p style={errStyle}>{errors.name.message}</p>}
                </div>
                <div>
                  <label style={labelStyle}>Industry</label>
                  <input className="input-field" placeholder="e.g. CleanTech" {...register('industry', { required: 'Required' })} />
                  {errors.industry && <p style={errStyle}>{errors.industry.message}</p>}
                </div>
              </div>
              <div>
                <label style={labelStyle}>Description</label>
                <textarea rows={3} className="input-field" style={{ height: 'auto', resize: 'vertical' }} placeholder="Describe your startup in a few sentences…" {...register('description', { required: 'Required' })} />
                {errors.description && <p style={errStyle}>{errors.description.message}</p>}
              </div>

              {sectionHeading('Details')}
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: 14 }}>
                <div>
                  <label style={labelStyle}>Problem statement</label>
                  <textarea rows={3} className="input-field" style={{ height: 'auto', resize: 'vertical' }} placeholder="What problem are you solving?" {...register('problemStatement', { required: 'Required' })} />
                  {errors.problemStatement && <p style={errStyle}>{errors.problemStatement.message}</p>}
                </div>
                <div>
                  <label style={labelStyle}>Your solution</label>
                  <textarea rows={3} className="input-field" style={{ height: 'auto', resize: 'vertical' }} placeholder="How do you solve it?" {...register('solution', { required: 'Required' })} />
                  {errors.solution && <p style={errStyle}>{errors.solution.message}</p>}
                </div>
              </div>

              {sectionHeading('Funding')}
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: 14 }}>
                <div>
                  <label style={labelStyle}>Funding goal (₹)</label>
                  <input type="number" className="input-field" placeholder="5000000" {...register('fundingGoal', { required: 'Required', min: 1 })} />
                  {errors.fundingGoal && <p style={errStyle}>{errors.fundingGoal.message}</p>}
                </div>
                <div>
                  <label style={labelStyle}>Stage</label>
                  <select className="input-field" {...register('stage', { required: 'Required' })}>
                    <option value="">Select stage</option>
                    <option value="IDEA">Idea</option>
                    <option value="MVP">MVP</option>
                    <option value="EARLY_TRACTION">Early Traction</option>
                    <option value="SCALING">Scaling</option>
                  </select>
                  {errors.stage && <p style={errStyle}>{errors.stage.message}</p>}
                </div>
                <div>
                  <label style={labelStyle}>Location</label>
                  <input className="input-field" placeholder="e.g. Bengaluru" {...register('location')} />
                </div>
              </div>
              <div style={{ display: 'flex', gap: 10, paddingTop: 8, borderTop: '1px solid var(--border)', marginTop: 4 }}>
                <Button type="submit" variant="primary" isLoading={isSubmitting}>Submit startup</Button>
                <Button type="button" variant="secondary" onClick={() => navigate(-1)}>Cancel</Button>
              </div>
            </form>
          </div>

          {/* Right — live preview */}
          <div style={{ position: 'sticky', top: 20 }}>
            <p style={{ fontSize: 12, fontWeight: 600, color: 'var(--text-muted)', marginBottom: 12, textTransform: 'uppercase', letterSpacing: '0.06em' }}>Live preview</p>
            <div style={{ background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: 16, padding: 20, boxShadow: 'var(--shadow-card)' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 14 }}>
                <div style={{ width: 42, height: 42, borderRadius: 10, background: 'var(--brand-subtle)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 16, fontWeight: 700, color: 'var(--brand)' }}>
                  {(watched.name || 'S')[0].toUpperCase()}
                </div>
                <div>
                  <p style={{ fontSize: 15, fontWeight: 600, color: 'var(--text-primary)', marginBottom: 3 }}>
                    {watched.name || <span style={{ color: 'var(--text-faint)' }}>Startup name</span>}
                  </p>
                  <div style={{ display: 'flex', gap: 5, flexWrap: 'wrap' }}>
                    {watched.industry && <span className="badge-blue" style={{ fontSize: 10 }}>{watched.industry}</span>}
                    {watched.stage && <span className="badge-green" style={{ fontSize: 10 }}>{watched.stage === 'EARLY_TRACTION' ? 'Early Traction' : watched.stage}</span>}
                    <span className="badge-yellow" style={{ fontSize: 10 }}>Pending review</span>
                  </div>
                </div>
              </div>
              <p style={{ fontSize: 12, color: 'var(--text-muted)', lineHeight: 1.6, marginBottom: 12, minHeight: 40 }}>
                {watched.description || <span style={{ color: 'var(--text-faint)' }}>Description will appear here…</span>}
              </p>
              {(watched.location || watched.fundingGoal) && (
                <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap', paddingTop: 12, borderTop: '1px solid var(--border)' }}>
                  {watched.location && <span style={{ fontSize: 11, color: 'var(--text-muted)' }}>📍 {watched.location}</span>}
                  {watched.fundingGoal && <span style={{ fontSize: 11, color: 'var(--text-muted)' }}>💰 ₹{Number(watched.fundingGoal).toLocaleString('en-IN')}</span>}
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
    </Layout>
  );
};

export default CreateStartup;
