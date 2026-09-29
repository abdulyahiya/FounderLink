// ─── Auth ────────────────────────────────────────────────────────────────────

export interface User {
  userId: number;
  role: string;
  email: string;
  name: string;
}

export interface AuthState {
  token: string | null;
  user: User | null;
  isAuthenticated: boolean;
}

export interface AuthResponse {
  token: string;
  refreshToken: string;
  userId: number;
  role: string;
  email: string;
  name: string;
}

export interface LoginFormData {
  email: string;
  password: string;
}

export interface RegisterFormData {
  name: string;
  email: string;
  password: string;
  role: 'ROLE_FOUNDER' | 'ROLE_INVESTOR' | 'ROLE_COFOUNDER';
}

export interface ForgotPasswordData {
  email: string;
}

export interface ResetPasswordData {
  token: string;
  newPassword: string;
}

export interface ChangePasswordData {
  oldPassword: string;
  newPassword: string;
}

// ─── Startup ─────────────────────────────────────────────────────────────────

export interface Startup {
  id: number;
  name: string;
  industry: string;
  description: string;
  problemStatement: string;
  solution: string;
  fundingGoal: number;
  stage: string;
  location: string;
  founderId: number;
  isApproved: boolean;
  isRejected: boolean;
  createdAt: string;
}

export interface StartupFormData {
  name: string;
  industry: string;
  description: string;
  problemStatement: string;
  solution: string;
  fundingGoal: number;
  stage: string;
  location: string;
}

export interface PaginatedStartups {
  content: Startup[];
  totalPages: number;
  totalElements: number;
}

export interface StartupState {
  items: Startup[];
  totalPages: number;
  totalElements: number;
  currentPage: number;
  loading: boolean;
  error: string | null;
}

// ─── Investment ──────────────────────────────────────────────────────────────

export interface Investment {
  id: number;
  investorId: number;
  startupId: number;
  amount: number;
  status: string;
  createdAt: string;
}

export interface InvestmentFormData {
  amount: number;
}

// ─── Payment ─────────────────────────────────────────────────────────────────

export interface Payment {
  id: number;
  investorId: number;
  founderId: number;
  startupId: number;
  startupName: string;
  investorName: string;
  investorEmail: string;
  founderEmail: string;
  amount: number;
  status: string;
  razorpayOrderId: string | null;
  razorpayPaymentId: string | null;
  createdAt: string;
}

export interface CreateOrderRequest {
  investorId: number;
  founderId: number;
  startupId: number;
  startupName: string;
  investorName: string;
  investorEmail: string;
  founderEmail: string;
  amount: number;
}

export interface CreateOrderResponse {
  orderId: string;
  amount: number;
  currency: string;
  keyId: string;
}

export interface VerifyPaymentRequest {
  razorpayOrderId: string;
  razorpayPaymentId: string;
  razorpaySignature: string;
}

export interface VerifyPaymentResponse {
  success: boolean;
}

// ─── Notification ────────────────────────────────────────────────────────────

export interface Notification {
  id: number;
  userId: number;
  message: string;
  type: string;
  isRead: boolean;
  createdAt: string;
}

export interface NotificationState {
  unreadCount: number;
}

// ─── Theme ───────────────────────────────────────────────────────────────────

export interface ThemeState {
  mode: string;
}

// ─── Messaging ───────────────────────────────────────────────────────────────

export interface Conversation {
  id: number;
  participant1Id: number;
  participant2Id: number;
  createdAt: string;
}

export interface Message {
  id: number;
  senderId: number;
  receiverId: number;
  content: string;
  conversationId: number;
  createdAt: string;
}

// ─── Team ────────────────────────────────────────────────────────────────────

export interface TeamMember {
  id: number;
  userId: number;
  startupId: number;
  role: string;
  status: string;
  joinedAt: string;
  createdAt: string;
}

export interface Invitation {
  id: number;
  startupId: number;
  invitedUserId: number;
  role: string;
  status: string;
  createdAt: string;
}

export interface InviteCoFounderData {
  startupId: number;
  invitedUserId: number;
  role: string;
}

// ─── User Profile ────────────────────────────────────────────────────────────

export interface UserProfile {
  userId: number;
  name: string;
  email: string;
  bio?: string;
  skills?: string;
  experience?: string;
  portfolioLinks?: string;
}

export interface AuthUser {
  userId: number;
  name: string;
  email: string;
  role: string;
}

export interface ProfileFormData {
  name?: string | null;
  bio?: string | null;
  skills?: string | null;
  experience?: string | null;
  portfolioLinks?: string | null;
}

export interface RouteLocationState {
  otherUserId?: number;
}

// ─── Redux Root State ────────────────────────────────────────────────────────

export interface RootState {
  auth: AuthState;
  notifications: NotificationState;
  theme: ThemeState;
  startups: StartupState;
}

// ─── API Response Wrapper ────────────────────────────────────────────────────

export interface ApiResponse<T> {
  status?: number;
  data: T;
  message?: string;
}

// ─── Razorpay ────────────────────────────────────────────────────────────────

export interface RazorpayOptions {
  key: string;
  amount: number;
  currency: string;
  name: string;
  description: string;
  order_id: string;
  handler: (_response: RazorpayResponse) => void;
  prefill: { name: string; email: string };
  theme: { color: string };
  modal: { ondismiss: () => void };
}

export interface RazorpayResponse {
  razorpay_order_id: string;
  razorpay_payment_id: string;
  razorpay_signature: string;
}

declare global {
  interface Window {
    Razorpay: new (_options: RazorpayOptions) => { open: () => void };
  }
}

// ─── Saga ────────────────────────────────────────────────────────────────────

export interface PaymentSaga {
  id: number;
  paymentId: number;
  status: string;
  steps: string[];
}
