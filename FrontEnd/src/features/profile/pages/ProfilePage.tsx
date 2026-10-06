import { useState, useEffect } from 'react';
import { useAppSelector } from '../../../redux/hooks';
import { selectUserType } from '../../../redux/selectors/authSelectors';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Badge } from '@/components/ui/badge';
import { Separator } from '@/components/ui/separator';
import { 
  User, Mail, Shield, ShieldCheck, Loader2, KeyRound, 
  Eye, EyeOff, Copy, Check, Calendar, AlertCircle, 
  Pencil, Lock, RefreshCw, ArrowRight
} from 'lucide-react';
import api from '../../../config/api';
import { toast } from 'sonner';
import { Link } from 'react-router-dom';

interface UserProfile {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  phone?: string;
  createdAt: string;
  updatedAt: string;
}

export default function ProfilePage() {
  const userType = useAppSelector(selectUserType);
  
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [isEditing, setIsEditing] = useState(false);
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [copiedId, setCopiedId] = useState(false);

  // Form state for profile details
  const [formData, setFormData] = useState({
    firstName: '',
    lastName: '',
    email: '',
    phone: '',
  });

  // Password change state
  const [passwordData, setPasswordData] = useState({
    currentPassword: '',
    newPassword: '',
    confirmPassword: '',
  });
  const [showCurrentPassword, setShowCurrentPassword] = useState(false);
  const [showNewPassword, setShowNewPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [isChangingPassword, setIsChangingPassword] = useState(false);
  const [passwordError, setPasswordError] = useState<string | null>(null);

  // Reset link via email state
  const [isSendingResetLink, setIsSendingResetLink] = useState(false);
  const [resetEmailSent, setResetEmailSent] = useState(false);

  useEffect(() => {
    fetchProfile();
  }, []);

  const fetchProfile = async () => {
    try {
      setIsLoading(true);
      const response = await api.get('/api/customers/profile');
      const data = response.data;
      
      setProfile(data);
      setFormData({
        firstName: data.firstName || '',
        lastName: data.lastName || '',
        email: data.email || '',
        phone: data.phone || '',
      });
    } catch (error: any) {
      console.error('Failed to fetch profile:', error);
      if (error.response?.status === 401) {
        toast.error('Session expired. Please log in again.');
      } else {
        toast.error(error.response?.data?.message || 'Failed to load profile');
      }
    } finally {
      setIsLoading(false);
    }
  };

  const handleSaveProfile = async (e?: React.FormEvent) => {
    if (e) e.preventDefault();
    if (!formData.firstName.trim() || !formData.lastName.trim()) {
      toast.error('First and last name are required');
      return;
    }
    if (!formData.email.trim()) {
      toast.error('Email is required');
      return;
    }

    try {
      setIsSaving(true);
      const response = await api.put('/api/customers/profile', formData);
      const updatedProfile = response.data;
      
      setProfile(updatedProfile);
      setIsEditing(false);
      toast.success('Profile updated successfully');
    } catch (error: any) {
      console.error('Failed to update profile:', error);
      toast.error(error.response?.data?.message || 'Failed to update profile');
    } finally {
      setIsSaving(false);
    }
  };

  const handleCancelEdit = () => {
    if (profile) {
      setFormData({
        firstName: profile.firstName || '',
        lastName: profile.lastName || '',
        email: profile.email || '',
        phone: profile.phone || '',
      });
    }
    setIsEditing(false);
  };

  const handleCopyId = () => {
    if (!profile?.id) return;
    navigator.clipboard.writeText(profile.id);
    setCopiedId(true);
    toast.success('User ID copied to clipboard');
    setTimeout(() => setCopiedId(false), 2000);
  };

  const handleChangePassword = async (e: React.FormEvent) => {
    e.preventDefault();
    setPasswordError(null);

    if (!passwordData.currentPassword) {
      setPasswordError('Please enter your current password');
      return;
    }
    if (passwordData.newPassword.length < 6) {
      setPasswordError('New password must be at least 6 characters long');
      return;
    }
    if (passwordData.newPassword === passwordData.currentPassword) {
      setPasswordError('New password cannot be the same as your current password');
      return;
    }
    if (passwordData.newPassword !== passwordData.confirmPassword) {
      setPasswordError('New password and confirm password do not match');
      return;
    }

    try {
      setIsChangingPassword(true);
      await api.put('/api/customers/change-password', {
        currentPassword: passwordData.currentPassword,
        newPassword: passwordData.newPassword,
      });

      toast.success('Password changed successfully!');
      setPasswordData({
        currentPassword: '',
        newPassword: '',
        confirmPassword: '',
      });
    } catch (error: any) {
      console.error('Password change failed:', error);
      const message = error.response?.data?.message || 'Failed to change password. Please check your current password.';
      setPasswordError(message);
      toast.error(message);
    } finally {
      setIsChangingPassword(false);
    }
  };

  const handleSendResetEmail = async () => {
    if (!profile?.email) return;

    try {
      setIsSendingResetLink(true);
      await api.post('/api/auth/password/forgot', { email: profile.email });
      setResetEmailSent(true);
      toast.success(`Password reset link sent to ${profile.email}`);
    } catch (error: any) {
      console.error('Failed to send reset email:', error);
      toast.error(error.response?.data?.message || 'Failed to dispatch reset email. Please try again.');
    } finally {
      setIsSendingResetLink(false);
    }
  };

  const getInitials = () => {
    if (!profile) return 'U';
    const first = profile.firstName ? profile.firstName[0].toUpperCase() : '';
    const last = profile.lastName ? profile.lastName[0].toUpperCase() : '';
    return first + last || 'U';
  };

  if (isLoading) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[60vh] gap-3">
        <Loader2 className="h-8 w-8 animate-spin text-primary" />
        <p className="text-sm text-muted-foreground animate-pulse">Loading your profile...</p>
      </div>
    );
  }

  if (!profile) {
    return (
      <div className="max-w-xl mx-auto py-16 px-4 text-center">
        <div className="bg-destructive/10 text-destructive p-4 rounded-xl inline-flex mb-4">
          <AlertCircle className="h-8 w-8" />
        </div>
        <h2 className="text-2xl font-bold mb-2">Unable to Load Profile</h2>
        <p className="text-muted-foreground mb-6">
          We encountered an issue fetching your account information.
        </p>
        <Button onClick={fetchProfile} className="gap-2">
          <RefreshCw className="h-4 w-4" /> Retry
        </Button>
      </div>
    );
  }

  const newPasswordMatch = 
    passwordData.confirmPassword.length > 0 && 
    passwordData.newPassword === passwordData.confirmPassword;
  
  const newPasswordMismatch = 
    passwordData.confirmPassword.length > 0 && 
    passwordData.newPassword !== passwordData.confirmPassword;

  return (
    <div className="w-full max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8 md:py-10 space-y-8">
      {/* Header section */}
      <div>
        <div className="flex items-center gap-2 mb-1">
          <Badge variant="outline" className="text-xs font-medium text-primary border-primary/30">
            Account Management
          </Badge>
          <span className="text-xs text-muted-foreground">• Customer Portal</span>
        </div>
        <h1 className="text-3xl font-bold tracking-tight text-foreground">Profile & Settings</h1>
        <p className="text-muted-foreground text-sm mt-1">
          View and update your personal details, credentials, and account security.
        </p>
      </div>

      {/* Hero Profile Banner Card */}
      <Card className="overflow-hidden border border-border/70 shadow-sm">
        <div className="h-24 bg-gradient-to-r from-primary/15 via-primary/5 to-transparent border-b border-border/40" />
        <CardContent className="relative pt-0 pb-6 px-6 sm:px-8">
          <div className="flex flex-col sm:flex-row sm:items-end justify-between gap-4 -mt-12">
            <div className="flex flex-col sm:flex-row items-start sm:items-end gap-4">
              {/* Avatar */}
              <div className="h-24 w-24 rounded-2xl bg-gradient-to-br from-primary to-primary/80 text-primary-foreground font-bold text-3xl flex items-center justify-center shadow-md ring-4 ring-background shrink-0">
                {getInitials()}
              </div>
              <div className="space-y-1">
                <div className="flex items-center gap-2 flex-wrap">
                  <h2 className="text-2xl font-bold tracking-tight">
                    {profile.firstName} {profile.lastName}
                  </h2>
                  <Badge variant="secondary" className="capitalize text-xs font-semibold">
                    {userType?.toLowerCase() || 'Customer'}
                  </Badge>
                  <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-medium bg-emerald-500/10 text-emerald-600 dark:text-emerald-400">
                    <span className="h-1.5 w-1.5 rounded-full bg-emerald-500 animate-pulse" />
                    Active Account
                  </span>
                </div>
                <p className="text-sm text-muted-foreground flex items-center gap-1.5">
                  <Mail className="h-3.5 w-3.5" />
                  {profile.email}
                </p>
              </div>
            </div>

            {/* Top Action */}
            <div className="flex items-center gap-2 self-start sm:self-end pt-2 sm:pt-0">
              {!isEditing ? (
                <Button 
                  onClick={() => setIsEditing(true)} 
                  variant="outline" 
                  className="gap-2 shadow-sm hover:bg-primary/5"
                >
                  <Pencil className="h-4 w-4" />
                  Edit Profile
                </Button>
              ) : (
                <div className="flex items-center gap-2">
                  <Button 
                    variant="outline" 
                    onClick={handleCancelEdit} 
                    disabled={isSaving}
                    size="sm"
                  >
                    Cancel
                  </Button>
                  <Button 
                    onClick={handleSaveProfile} 
                    disabled={isSaving}
                    size="sm"
                    className="gap-2"
                  >
                    {isSaving ? (
                      <>
                        <Loader2 className="h-3.5 w-3.5 animate-spin" />
                        Saving...
                      </>
                    ) : (
                      'Save Changes'
                    )}
                  </Button>
                </div>
              )}
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Personal Information */}
      <Card className="border border-border/70 shadow-sm">
        <CardHeader className="pb-4">
          <div className="flex items-center justify-between">
            <div className="space-y-1">
              <CardTitle className="text-lg font-semibold flex items-center gap-2">
                <User className="h-5 w-5 text-primary" />
                Personal Information
              </CardTitle>
              <CardDescription>
                Your contact information and identification details
              </CardDescription>
            </div>
          </div>
        </CardHeader>
        <CardContent>
          {isEditing ? (
            <form onSubmit={handleSaveProfile} className="space-y-4">
              <div className="grid gap-4 sm:grid-cols-2">
                <div className="space-y-2">
                  <Label htmlFor="firstName" className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                    First Name *
                  </Label>
                  <Input
                    id="firstName"
                    value={formData.firstName}
                    onChange={(e) => setFormData({ ...formData, firstName: e.target.value })}
                    disabled={isSaving}
                    placeholder="Enter first name"
                    required
                  />
                </div>

                <div className="space-y-2">
                  <Label htmlFor="lastName" className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                    Last Name *
                  </Label>
                  <Input
                    id="lastName"
                    value={formData.lastName}
                    onChange={(e) => setFormData({ ...formData, lastName: e.target.value })}
                    disabled={isSaving}
                    placeholder="Enter last name"
                    required
                  />
                </div>

                <div className="space-y-2">
                  <Label htmlFor="email" className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                    Email Address *
                  </Label>
                  <Input
                    id="email"
                    type="email"
                    value={formData.email}
                    onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                    disabled={isSaving}
                    placeholder="name@example.com"
                    required
                  />
                </div>

                <div className="space-y-2">
                  <Label htmlFor="phone" className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                    Phone Number
                  </Label>
                  <Input
                    id="phone"
                    type="tel"
                    value={formData.phone}
                    onChange={(e) => setFormData({ ...formData, phone: e.target.value })}
                    disabled={isSaving}
                    placeholder="+1 (555) 000-0000"
                  />
                </div>
              </div>

              <div className="flex justify-end gap-2 pt-4">
                <Button type="button" variant="outline" onClick={handleCancelEdit} disabled={isSaving}>
                  Cancel
                </Button>
                <Button type="submit" disabled={isSaving} className="gap-2">
                  {isSaving && <Loader2 className="h-4 w-4 animate-spin" />}
                  Save Changes
                </Button>
              </div>
            </form>
          ) : (
            <div className="grid gap-4 sm:grid-cols-2">
              <div className="p-4 rounded-xl bg-muted/40 border border-border/50 flex flex-col justify-between">
                <span className="text-xs font-semibold uppercase tracking-wider text-muted-foreground mb-1">
                  First Name
                </span>
                <span className="text-base font-medium text-foreground">{profile.firstName}</span>
              </div>

              <div className="p-4 rounded-xl bg-muted/40 border border-border/50 flex flex-col justify-between">
                <span className="text-xs font-semibold uppercase tracking-wider text-muted-foreground mb-1">
                  Last Name
                </span>
                <span className="text-base font-medium text-foreground">{profile.lastName}</span>
              </div>

              <div className="p-4 rounded-xl bg-muted/40 border border-border/50 flex flex-col justify-between">
                <span className="text-xs font-semibold uppercase tracking-wider text-muted-foreground mb-1">
                  Email Address
                </span>
                <span className="text-base font-medium text-foreground truncate">{profile.email}</span>
              </div>

              <div className="p-4 rounded-xl bg-muted/40 border border-border/50 flex flex-col justify-between">
                <span className="text-xs font-semibold uppercase tracking-wider text-muted-foreground mb-1">
                  Phone Number
                </span>
                <span className={`text-base font-medium ${profile.phone ? 'text-foreground' : 'text-muted-foreground italic'}`}>
                  {profile.phone || 'Not provided'}
                </span>
              </div>
            </div>
          )}
        </CardContent>
      </Card>

      {/* Account Details & Metadata */}
      <Card className="border border-border/70 shadow-sm">
        <CardHeader className="pb-4">
          <CardTitle className="text-lg font-semibold flex items-center gap-2">
            <ShieldCheck className="h-5 w-5 text-primary" />
            Account Overview
          </CardTitle>
          <CardDescription>
            System identifiers and membership details associated with your profile
          </CardDescription>
        </CardHeader>
        <CardContent>
          <div className="grid gap-4 sm:grid-cols-3">
            {/* User ID */}
            <div className="p-4 rounded-xl bg-muted/40 border border-border/50 flex flex-col justify-between">
              <span className="text-xs font-semibold uppercase tracking-wider text-muted-foreground mb-1">
                Account ID
              </span>
              <div className="flex items-center justify-between gap-2 mt-1">
                <span className="font-mono text-xs text-foreground truncate" title={profile.id}>
                  {profile.id}
                </span>
                <Button 
                  variant="ghost" 
                  size="icon" 
                  className="h-7 w-7 text-muted-foreground hover:text-foreground shrink-0" 
                  onClick={handleCopyId}
                  title="Copy User ID"
                >
                  {copiedId ? <Check className="h-3.5 w-3.5 text-emerald-600" /> : <Copy className="h-3.5 w-3.5" />}
                </Button>
              </div>
            </div>

            {/* Account Type */}
            <div className="p-4 rounded-xl bg-muted/40 border border-border/50 flex flex-col justify-between">
              <span className="text-xs font-semibold uppercase tracking-wider text-muted-foreground mb-1">
                Account Type
              </span>
              <div className="flex items-center gap-2 mt-1">
                <Shield className="h-4 w-4 text-primary" />
                <span className="text-sm font-semibold capitalize">
                  {userType?.toLowerCase() || 'Customer'}
                </span>
              </div>
            </div>

            {/* Member Since */}
            <div className="p-4 rounded-xl bg-muted/40 border border-border/50 flex flex-col justify-between">
              <span className="text-xs font-semibold uppercase tracking-wider text-muted-foreground mb-1">
                Member Since
              </span>
              <div className="flex items-center gap-2 mt-1">
                <Calendar className="h-4 w-4 text-muted-foreground" />
                <span className="text-sm font-medium">
                  {new Date(profile.createdAt).toLocaleDateString('en-US', {
                    year: 'numeric',
                    month: 'long',
                    day: 'numeric',
                  })}
                </span>
              </div>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Security & Password Management */}
      <Card className="border border-border/70 shadow-sm overflow-hidden">
        <CardHeader className="border-b border-border/40 bg-muted/20">
          <div className="space-y-1">
            <CardTitle className="text-lg font-semibold flex items-center gap-2">
              <Lock className="h-5 w-5 text-primary" />
              Security & Password
            </CardTitle>
            <CardDescription>
              Change your password directly or request a reset link sent to your email
            </CardDescription>
          </div>
        </CardHeader>
        <CardContent className="p-6 space-y-8">
          {/* Method 1: Change Password Directly */}
          <div className="space-y-4">
            <div className="flex items-center gap-2">
              <div className="h-8 w-8 rounded-lg bg-primary/10 flex items-center justify-center text-primary">
                <KeyRound className="h-4 w-4" />
              </div>
              <div>
                <h3 className="text-base font-semibold text-foreground">Change Password</h3>
                <p className="text-xs text-muted-foreground">
                  Update your current password immediately. You will stay signed in.
                </p>
              </div>
            </div>

            {passwordError && (
              <div className="p-3 rounded-lg bg-destructive/10 border border-destructive/20 text-destructive text-sm flex items-center gap-2">
                <AlertCircle className="h-4 w-4 shrink-0" />
                <span>{passwordError}</span>
              </div>
            )}

            <form onSubmit={handleChangePassword} className="space-y-4 pt-2">
              <div className="grid gap-4 sm:grid-cols-3">
                {/* Current Password */}
                <div className="space-y-2">
                  <Label htmlFor="currentPassword">Current Password</Label>
                  <div className="relative">
                    <Input
                      id="currentPassword"
                      type={showCurrentPassword ? 'text' : 'password'}
                      value={passwordData.currentPassword}
                      onChange={(e) => {
                        setPasswordData({ ...passwordData, currentPassword: e.target.value });
                        if (passwordError) setPasswordError(null);
                      }}
                      placeholder="Enter current password"
                      required
                      className="pr-10"
                    />
                    <button
                      type="button"
                      onClick={() => setShowCurrentPassword(!showCurrentPassword)}
                      className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground"
                      tabIndex={-1}
                    >
                      {showCurrentPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                    </button>
                  </div>
                </div>

                {/* New Password */}
                <div className="space-y-2">
                  <Label htmlFor="newPassword">New Password</Label>
                  <div className="relative">
                    <Input
                      id="newPassword"
                      type={showNewPassword ? 'text' : 'password'}
                      value={passwordData.newPassword}
                      onChange={(e) => {
                        setPasswordData({ ...passwordData, newPassword: e.target.value });
                        if (passwordError) setPasswordError(null);
                      }}
                      placeholder="Min 6 characters"
                      required
                      className="pr-10"
                    />
                    <button
                      type="button"
                      onClick={() => setShowNewPassword(!showNewPassword)}
                      className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground"
                      tabIndex={-1}
                    >
                      {showNewPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                    </button>
                  </div>
                  {passwordData.newPassword && passwordData.newPassword.length < 6 && (
                    <p className="text-xs text-amber-500">Must be at least 6 characters</p>
                  )}
                </div>

                {/* Confirm Password */}
                <div className="space-y-2">
                  <Label htmlFor="confirmPassword">Confirm New Password</Label>
                  <div className="relative">
                    <Input
                      id="confirmPassword"
                      type={showConfirmPassword ? 'text' : 'password'}
                      value={passwordData.confirmPassword}
                      onChange={(e) => {
                        setPasswordData({ ...passwordData, confirmPassword: e.target.value });
                        if (passwordError) setPasswordError(null);
                      }}
                      placeholder="Repeat new password"
                      required
                      className="pr-10"
                    />
                    <button
                      type="button"
                      onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                      className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground"
                      tabIndex={-1}
                    >
                      {showConfirmPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                    </button>
                  </div>
                  {newPasswordMatch && (
                    <p className="text-xs text-emerald-600 flex items-center gap-1">
                      <Check className="h-3 w-3" /> Passwords match
                    </p>
                  )}
                  {newPasswordMismatch && (
                    <p className="text-xs text-destructive">Passwords do not match</p>
                  )}
                </div>
              </div>

              <div className="flex justify-end pt-2">
                <Button 
                  type="submit" 
                  disabled={isChangingPassword || !passwordData.currentPassword || !passwordData.newPassword || !passwordData.confirmPassword}
                  className="gap-2"
                >
                  {isChangingPassword ? (
                    <>
                      <Loader2 className="h-4 w-4 animate-spin" />
                      Updating Password...
                    </>
                  ) : (
                    <>
                      <KeyRound className="h-4 w-4" />
                      Update Password
                    </>
                  )}
                </Button>
              </div>
            </form>
          </div>

          <Separator />

          {/* Method 2: Forgot Password / Email Reset Option */}
          <div className="rounded-xl border border-border/60 bg-muted/30 p-5 space-y-3">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
              <div className="space-y-1">
                <h4 className="text-sm font-semibold flex items-center gap-2">
                  <Mail className="h-4 w-4 text-primary" />
                  Forgot your current password?
                </h4>
                <p className="text-xs text-muted-foreground max-w-lg">
                  Send a password reset link to your verified email address (<span className="font-medium text-foreground">{profile.email}</span>).
                </p>
              </div>

              <Button
                variant="outline"
                size="sm"
                onClick={handleSendResetEmail}
                disabled={isSendingResetLink}
                className="shrink-0 gap-2 border-primary/20 hover:bg-primary/5"
              >
                {isSendingResetLink ? (
                  <>
                    <Loader2 className="h-3.5 w-3.5 animate-spin" />
                    Sending link...
                  </>
                ) : (
                  <>
                    <Mail className="h-3.5 w-3.5" />
                    Send Reset Link
                  </>
                )}
              </Button>
            </div>

            {resetEmailSent && (
              <div className="p-3 rounded-lg bg-emerald-500/10 border border-emerald-500/20 text-emerald-700 dark:text-emerald-400 text-xs flex items-center justify-between gap-2">
                <div className="flex items-center gap-2">
                  <Check className="h-4 w-4 shrink-0 text-emerald-600" />
                  <span>
                    A password reset email has been dispatched to <strong>{profile.email}</strong>.
                  </span>
                </div>
                <Link 
                  to="/reset-password" 
                  className="inline-flex items-center gap-1 font-semibold underline hover:no-underline"
                >
                  Enter token here <ArrowRight className="h-3 w-3" />
                </Link>
              </div>
            )}
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
