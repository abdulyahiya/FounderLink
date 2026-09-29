import React from 'react';
import { RouterProvider } from 'react-router-dom';
import { Toaster } from 'react-hot-toast';
import { Provider } from 'react-redux';
import store from './store/store';
import router from './routes/router';

function App(): React.ReactElement {
  return (
    <Provider store={store}>
      <Toaster position="top-right" toastOptions={{ duration: 3000 }} />
      <RouterProvider router={router} />
    </Provider>
  );
}

export default App;
