import { createSlice, PayloadAction } from '@reduxjs/toolkit';
import { NotificationState } from '../../types';

const notificationSlice = createSlice({
  name: 'notifications',
  initialState: { unreadCount: 0 } as NotificationState,
  reducers: {
    setUnreadCount: (state, action: PayloadAction<number>) => { state.unreadCount = action.payload; },
    decrementUnread: (state) => { if (state.unreadCount > 0) state.unreadCount--; },
  },
});

export const { setUnreadCount, decrementUnread } = notificationSlice.actions;
export default notificationSlice.reducer;
