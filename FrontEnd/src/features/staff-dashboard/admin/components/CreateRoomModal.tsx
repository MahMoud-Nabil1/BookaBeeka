import { useState, useRef, useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import {
  Tag,
  Image as ImageIcon,
  Upload,
  X,
  Loader2,
  Check,
  Plus,
  Star,
  FileText,
} from 'lucide-react';
import { useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
} from '../../../../components/ui/dialog';
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from '../../../../components/ui/form';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '../../../../components/ui/select';
import { Input } from '../../../../components/ui/input';
import { Textarea } from '../../../../components/ui/textarea';
import { Button } from '../../../../components/ui/button';
import { Badge } from '../../../../components/ui/badge';
import { useCreateRoom } from '../../../catalog/hooks/useInventory';
import { useAmenities } from '../../../catalog/hooks/useAmenities';
import { amenityApi } from '../../../catalog/api/amenityApi';
import { mediaApi } from '../../../catalog/api/mediaApi';
import { useAppSelector } from '../../../../redux/hooks';
import { selectTenantId } from '../../../../redux/selectors/authSelectors';

const ROOM_CATEGORIES = ['STANDARD', 'DELUXE', 'SUITE', 'PENTHOUSE', 'VILLA'] as const;

const schema = z.object({
  name: z.string().min(1, 'Room name is required').max(100, 'Name is too long'),
  roomCategory: z.string().min(1, 'Category is required'),
  capacity: z.coerce.number().int().min(1, 'Capacity must be at least 1').max(50),
  pricePerNight: z.coerce.number().min(0, 'Price must be 0 or more').optional(),
  description: z.string().max(2000, 'Description is too long').optional(),
});

type FormData = z.infer<typeof schema>;

interface Props {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

interface PhotoPreview {
  file: File;
  previewUrl: string;
}

export function CreateRoomModal({ open, onOpenChange }: Props) {
  const queryClient = useQueryClient();
  const createRoom = useCreateRoom();
  const tenantId = useAppSelector(selectTenantId);

  // Available amenities for the hotel
  const { data: allAmenities = [], isLoading: loadingAmenities } = useAmenities();
  const [selectedAmenityIds, setSelectedAmenityIds] = useState<Set<string>>(new Set());

  // Selected photo files
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [selectedPhotos, setSelectedPhotos] = useState<PhotoPreview[]>([]);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submitStatus, setSubmitStatus] = useState<string>('');

  const form = useForm<FormData>({
    resolver: zodResolver(schema),
    defaultValues: {
      name: '',
      roomCategory: '',
      capacity: 2,
      pricePerNight: undefined,
      description: '',
    },
  });

  // Clean up object URLs when modal closes or photos change
  useEffect(() => {
    return () => {
      selectedPhotos.forEach((p) => URL.revokeObjectURL(p.previewUrl));
    };
  }, [selectedPhotos]);

  // Reset local state on close
  useEffect(() => {
    if (!open) {
      form.reset();
      setSelectedAmenityIds(new Set());
      setSelectedPhotos([]);
      setIsSubmitting(false);
      setSubmitStatus('');
    }
  }, [open, form]);

  const toggleAmenity = (amenityId: string) => {
    setSelectedAmenityIds((prev) => {
      const next = new Set(prev);
      if (next.has(amenityId)) {
        next.delete(amenityId);
      } else {
        next.add(amenityId);
      }
      return next;
    });
  };

  const handleFileSelect = (event: React.ChangeEvent<HTMLInputElement>) => {
    const files = event.target.files;
    if (!files || files.length === 0) return;

    const newPhotos: PhotoPreview[] = [];
    for (let i = 0; i < files.length; i++) {
      const file = files[i];
      if (!file.type.startsWith('image/')) {
        toast.error(`${file.name} is not an image file`);
        continue;
      }
      if (file.size > 10 * 1024 * 1024) {
        toast.error(`${file.name} exceeds 10MB limit`);
        continue;
      }
      newPhotos.push({
        file,
        previewUrl: URL.createObjectURL(file),
      });
    }

    setSelectedPhotos((prev) => [...prev, ...newPhotos]);
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };

  const removePhoto = (index: number) => {
    setSelectedPhotos((prev) => {
      const target = prev[index];
      if (target) {
        URL.revokeObjectURL(target.previewUrl);
      }
      return prev.filter((_, i) => i !== index);
    });
  };

  const onSubmit = async (data: FormData) => {
    if (!tenantId) {
      toast.error('Missing tenant credentials');
      return;
    }

    setIsSubmitting(true);
    try {
      setSubmitStatus('Creating room details…');

      // 1. Create room entity
      const createdRoom = await createRoom.mutateAsync({
        tenantId,
        name: data.name,
        roomCategory: data.roomCategory,
        capacity: data.capacity,
        specs: {
          ...(data.pricePerNight != null ? { pricePerNight: data.pricePerNight } : {}),
          ...(data.description?.trim() ? { description: data.description.trim() } : {}),
        },
      });

      const roomId = createdRoom.id;

      // 2. Link selected amenities
      if (selectedAmenityIds.size > 0) {
        setSubmitStatus(`Linking ${selectedAmenityIds.size} amenities…`);
        const amenityPromises = Array.from(selectedAmenityIds).map((amenityId) =>
          amenityApi.linkAmenityToRoom(roomId, amenityId, tenantId).catch((err) => {
            console.warn(`Failed to link amenity ${amenityId}:`, err);
          })
        );
        await Promise.all(amenityPromises);
      }

      // 3. Upload selected photos
      if (selectedPhotos.length > 0) {
        for (let i = 0; i < selectedPhotos.length; i++) {
          const { file } = selectedPhotos[i];
          setSubmitStatus(`Uploading photo ${i + 1} of ${selectedPhotos.length}…`);

          try {
            // Get signed Cloudinary parameters
            const signature = await mediaApi.getUploadSignature(roomId);

            // Upload directly to Cloudinary
            const formData = new FormData();
            formData.append('file', file);
            formData.append('signature', signature.signature);
            formData.append('timestamp', signature.timestamp.toString());
            formData.append('api_key', signature.apiKey);
            formData.append('folder', signature.folder);

            const cloudinaryUrl = `https://api.cloudinary.com/v1_1/${signature.cloudName}/image/upload`;
            const uploadRes = await fetch(cloudinaryUrl, {
              method: 'POST',
              body: formData,
            });

            if (!uploadRes.ok) {
              throw new Error(`Upload failed for ${file.name}`);
            }

            const cloudinaryData = await uploadRes.json();

            // Attach photo metadata to room
            await mediaApi.attachPhoto(roomId, {
              url: cloudinaryData.secure_url,
              publicId: cloudinaryData.public_id,
              isPrimary: i === 0,
            });
          } catch (photoErr) {
            console.error(`Error uploading photo ${file.name}:`, photoErr);
            toast.error(`Could not upload photo: ${file.name}`);
          }
        }
      }

      // Refresh queries
      queryClient.invalidateQueries({ queryKey: ['rooms'] });
      queryClient.invalidateQueries({ queryKey: ['room-amenities', roomId] });
      queryClient.invalidateQueries({ queryKey: ['photos', roomId] });

      toast.success('Room created successfully!', {
        description: `"${data.name}" is now ready in your inventory.`,
      });

      onOpenChange(false);
    } catch (err: any) {
      console.error('Error creating room:', err);
      toast.error(err?.message || 'Failed to create room. Please check your inputs.');
    } finally {
      setIsSubmitting(false);
      setSubmitStatus('');
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-[620px] max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle className="text-xl font-bold">Add New Room</DialogTitle>
          <DialogDescription>
            Create a room, set amenities, upload photos, and add description.
          </DialogDescription>
        </DialogHeader>

        <Form {...form}>
          <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-6">
            {/* ── Basic Info ── */}
            <div className="space-y-4">
              <FormField
                control={form.control}
                name="name"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Room Name *</FormLabel>
                    <FormControl>
                      <Input placeholder="e.g., Room 101, Deluxe Suite 205" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="roomCategory"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Category *</FormLabel>
                    <Select onValueChange={field.onChange} value={field.value}>
                      <FormControl>
                        <SelectTrigger>
                          <SelectValue placeholder="Select a category" />
                        </SelectTrigger>
                      </FormControl>
                      <SelectContent>
                        {ROOM_CATEGORIES.map((cat) => (
                          <SelectItem key={cat} value={cat}>
                            {cat.charAt(0) + cat.slice(1).toLowerCase()}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <div className="grid grid-cols-2 gap-4">
                <FormField
                  control={form.control}
                  name="capacity"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Capacity (guests) *</FormLabel>
                      <FormControl>
                        <Input type="number" min={1} max={50} {...field} />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />

                <FormField
                  control={form.control}
                  name="pricePerNight"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Price / Night ($)</FormLabel>
                      <FormControl>
                        <Input
                          type="number"
                          min={0}
                          step="0.01"
                          placeholder="0.00"
                          {...field}
                          value={field.value ?? ''}
                        />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
              </div>

              {/* ── Room Description ── */}
              <FormField
                control={form.control}
                name="description"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel className="flex items-center gap-1.5">
                      <FileText className="h-4 w-4 text-primary" />
                      Room Description
                    </FormLabel>
                    <FormControl>
                      <Textarea
                        placeholder="Describe the room, its view, furnishings, atmosphere, and key features..."
                        rows={3}
                        className="resize-none"
                        {...field}
                      />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </div>

            {/* ── Amenities Selection ── */}
            <div className="space-y-3 pt-2 border-t">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <Tag className="h-4 w-4 text-primary" />
                  <span className="font-semibold text-sm">Room Amenities</span>
                </div>
                <span className="text-xs text-muted-foreground">
                  {selectedAmenityIds.size} selected
                </span>
              </div>

              {loadingAmenities ? (
                <div className="flex items-center gap-2 text-xs text-muted-foreground py-2">
                  <Loader2 className="h-3.5 w-3.5 animate-spin" />
                  Loading hotel amenities…
                </div>
              ) : allAmenities.length === 0 ? (
                <p className="text-xs text-muted-foreground italic">
                  No amenities found in hotel catalog. You can create amenities from the Amenities tab.
                </p>
              ) : (
                <div className="flex flex-wrap gap-2 max-h-36 overflow-y-auto p-1 border rounded-lg bg-muted/20">
                  {allAmenities.map((amenity) => {
                    const isSelected = selectedAmenityIds.has(amenity.id);
                    return (
                      <button
                        key={amenity.id}
                        type="button"
                        onClick={() => toggleAmenity(amenity.id)}
                        className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-md text-xs font-medium transition-all cursor-pointer border ${
                          isSelected
                            ? 'bg-primary text-primary-foreground border-primary shadow-xs'
                            : 'bg-background hover:bg-muted text-foreground border-border'
                        }`}
                      >
                        {isSelected ? (
                          <Check className="h-3 w-3" />
                        ) : (
                          <Plus className="h-3 w-3 opacity-60" />
                        )}
                        <span>{amenity.name}</span>
                      </button>
                    );
                  })}
                </div>
              )}
            </div>

            {/* ── Photos Upload Selection ── */}
            <div className="space-y-3 pt-2 border-t">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <ImageIcon className="h-4 w-4 text-primary" />
                  <span className="font-semibold text-sm">Room Photos</span>
                </div>
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  className="h-7 text-xs gap-1"
                  onClick={() => fileInputRef.current?.click()}
                  disabled={isSubmitting}
                >
                  <Upload className="h-3.5 w-3.5" />
                  Select Photos
                </Button>
                <input
                  ref={fileInputRef}
                  type="file"
                  multiple
                  accept="image/*"
                  className="hidden"
                  onChange={handleFileSelect}
                />
              </div>

              {selectedPhotos.length === 0 ? (
                <div
                  onClick={() => fileInputRef.current?.click()}
                  className="border border-dashed rounded-lg p-4 text-center cursor-pointer hover:bg-muted/30 transition-colors"
                >
                  <Upload className="h-6 w-6 text-muted-foreground/60 mx-auto mb-1" />
                  <p className="text-xs font-medium text-foreground">Click to upload room photos</p>
                  <p className="text-[11px] text-muted-foreground">PNG, JPG, WebP up to 10MB each</p>
                </div>
              ) : (
                <div className="grid grid-cols-3 sm:grid-cols-4 gap-2.5 p-2 border rounded-lg bg-muted/20 max-h-48 overflow-y-auto">
                  {selectedPhotos.map((photo, index) => (
                    <div
                      key={photo.previewUrl}
                      className="relative rounded-md overflow-hidden aspect-video border group bg-black/5"
                    >
                      <img
                        src={photo.previewUrl}
                        alt={`Preview ${index + 1}`}
                        className="w-full h-full object-cover"
                      />
                      {index === 0 && (
                        <Badge
                          variant="default"
                          className="absolute top-1 left-1 text-[9px] px-1 py-0 h-4 bg-primary/90 gap-0.5"
                        >
                          <Star className="h-2.5 w-2.5 fill-current" /> Cover
                        </Badge>
                      )}
                      <button
                        type="button"
                        onClick={() => removePhoto(index)}
                        className="absolute top-1 right-1 h-5 w-5 rounded-full bg-black/60 hover:bg-black/90 text-white flex items-center justify-center transition-opacity opacity-80 hover:opacity-100"
                        title="Remove photo"
                      >
                        <X className="h-3 w-3" />
                      </button>
                    </div>
                  ))}
                </div>
              )}
            </div>

            {/* ── Footer ── */}
            <div className="flex items-center justify-between pt-4 border-t">
              <div className="text-xs text-muted-foreground">
                {isSubmitting && submitStatus && (
                  <span className="flex items-center gap-1.5 text-primary font-medium">
                    <Loader2 className="h-3.5 w-3.5 animate-spin" />
                    {submitStatus}
                  </span>
                )}
              </div>

              <div className="flex gap-2">
                <Button
                  type="button"
                  variant="outline"
                  onClick={() => onOpenChange(false)}
                  disabled={isSubmitting}
                >
                  Cancel
                </Button>
                <Button type="submit" disabled={isSubmitting}>
                  {isSubmitting ? (
                    <>
                      <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                      Creating…
                    </>
                  ) : (
                    'Create Room'
                  )}
                </Button>
              </div>
            </div>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  );
}
