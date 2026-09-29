import { useSelector } from 'react-redux';
import { selectCurrentUser, selectIsAuthenticated } from '../../store/slices/authSlice';
import { User } from '../../types';

interface UseAuthReturn {
  user: User | null;
  isAuthenticated: boolean;
  isFounder: boolean;
  isInvestor: boolean;
  isAdmin: boolean;
  isCoFounder: boolean;
  userId: number | undefined;
  role: string | undefined;
}

const useAuth = (): UseAuthReturn => {
  const user = useSelector(selectCurrentUser);
  const isAuthenticated = useSelector(selectIsAuthenticated);

  return {
    user,
    isAuthenticated,
    isFounder: user?.role === 'ROLE_FOUNDER',
    isInvestor: user?.role === 'ROLE_INVESTOR',
    isAdmin: user?.role === 'ROLE_ADMIN',
    isCoFounder: user?.role === 'ROLE_COFOUNDER',
    userId: user?.userId,
    role: user?.role,
  };
};

export default useAuth;
