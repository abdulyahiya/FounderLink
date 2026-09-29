import React from 'react';
import { Loader2 } from 'lucide-react';

type ButtonVariant = 'primary' | 'secondary' | 'danger' | 'success' | 'ghost';
type ButtonSize = 'sm' | 'md' | 'lg';

interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant;
  size?: ButtonSize;
  isLoading?: boolean;
  leftIcon?: React.ReactNode;
  rightIcon?: React.ReactNode;
  fullWidth?: boolean;
}

const variantClass: Record<ButtonVariant, string> = {
  primary:   'btn-primary',
  secondary: 'btn-secondary',
  danger:    'btn-danger',
  success:   'btn-success',
  ghost: 'inline-flex items-center justify-center gap-2 text-sm font-medium transition-colors active:scale-[0.98]',
};

const sizeClass: Record<ButtonSize, string> = {
  sm: '!py-1.5 !px-3 !text-xs',
  md: '',
  lg: '!py-3 !px-7 !text-base',
};

const Button: React.FC<ButtonProps> = ({
  variant = 'primary',
  size = 'md',
  isLoading = false,
  leftIcon,
  rightIcon,
  fullWidth = false,
  disabled,
  className = '',
  children,
  ...rest
}) => (
  <button
    disabled={disabled || isLoading}
    className={`${variantClass[variant]} ${sizeClass[size]} ${fullWidth ? 'w-full' : ''} ${className}`.trim()}
    {...rest}
  >
    {isLoading ? <Loader2 size={15} className="animate-spin" /> : leftIcon}
    {children}
    {!isLoading && rightIcon}
  </button>
);

export default Button;
