import { useEffect } from 'react'
import { useAppDispatch } from './redux/hooks'
import { initFromStorage } from './redux/slices/authSlice'
import AppRouter from './routes/AppRouter'
import { Toaster } from 'sonner'
import './App.css'

function App() {
  const dispatch = useAppDispatch()

  // Rehydrate auth state from localStorage on first load
  useEffect(() => {
    dispatch(initFromStorage())
  }, [dispatch])

  return (
    <>
      <AppRouter />
      <Toaster position="top-right" richColors />
    </>
  )
}

export default App
