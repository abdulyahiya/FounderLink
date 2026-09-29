import * as yup from 'yup';

export const loginSchema = yup.object({
  email: yup.string().email('Invalid email address').required('Email is required'),
  password: yup.string().min(6, 'Password must be at least 6 characters').required('Password is required'),
});

export const registerSchema = yup.object({
  name: yup.string().min(2, 'Name must be at least 2 characters').required('Name is required'),
  email: yup.string().email('Invalid email address').required('Email is required'),
  password: yup.string().min(6, 'Password must be at least 6 characters').required('Password is required'),
  role: yup.string().oneOf(['ROLE_FOUNDER', 'ROLE_INVESTOR', 'ROLE_COFOUNDER'], 'Please select a role').required('Role is required'),
});

export const startupSchema = yup.object({
  name: yup.string().min(2, 'Name must be at least 2 characters').required('Startup name is required'),
  industry: yup.string().required('Industry is required'),
  description: yup.string().min(20, 'Description must be at least 20 characters').required('Description is required'),
  problemStatement: yup.string().min(10, 'Problem statement must be at least 10 characters').required('Problem statement is required'),
  solution: yup.string().min(10, 'Solution must be at least 10 characters').required('Solution is required'),
  fundingGoal: yup.number().typeError('Funding goal must be a number').positive('Funding goal must be positive').required('Funding goal is required'),
  stage: yup.string().required('Stage is required'),
  location: yup.string().required('Location is required'),
});

export const investmentSchema = yup.object({
  amount: yup.number().typeError('Amount must be a number').positive('Amount must be positive').min(1000, 'Minimum investment is ₹1,000').required('Amount is required'),
});

export const profileSchema = yup.object({
  name: yup.string().min(2, 'Name must be at least 2 characters').nullable(),
  bio: yup.string().max(500, 'Bio must be under 500 characters').nullable(),
  skills: yup.string().nullable(),
  experience: yup.string().nullable(),
  portfolioLinks: yup.string().url('Must be a valid URL').nullable().transform((v: string | null) => v === '' ? null : v),
});
